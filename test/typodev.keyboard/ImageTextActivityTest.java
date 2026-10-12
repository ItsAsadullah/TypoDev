package typodev.keyboard;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.EditText;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.io.File;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import typodev.keyboard.ai.GeminiAiService;
import typodev.keyboard.ai.ImageTextActivity;
import typodev.keyboard.ai.ImageTextExtractor;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ImageTextActivityTest {
  private ImageTextActivity activity;
  private ClipboardManager clipboard;
  @Before public void setup() {
    activity = Robolectric.buildActivity(ImageTextActivity.class).setup().get();
    clipboard = (ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE);
    clipboard.setPrimaryClip(ClipData.newPlainText("previous", "Keep existing clipboard"));
  }
  private Object field(String name) throws Exception {
    Field field = ImageTextActivity.class.getDeclaredField(name); field.setAccessible(true); return field.get(activity);
  }
  private Object invoke(String name, Class<?>[] types, Object... arguments) throws Exception {
    Method method = ImageTextActivity.class.getDeclaredMethod(name, types); method.setAccessible(true);
    return method.invoke(activity, arguments);
  }
  private void result(String text) throws Exception {
    invoke("showResult", new Class<?>[]{String.class, boolean.class}, text, true);
  }
  @Test public void extractsOriginalBengaliAndEnglishIntoClipboardAndAllowsCorrections() throws Exception {
    String original = "বাংলা লেখা\nMeeting 10:30\nprice = 3.14;";
    result(original);
    assertEquals(original, clipboard.getPrimaryClip().getItemAt(0).getText().toString());
    EditText editor = (EditText)field("text"); editor.setText("সংশোধিত লেখা");
    ((android.widget.Button)field("copy")).performClick();
    assertEquals("সংশোধিত লেখা", clipboard.getPrimaryClip().getItemAt(0).getText().toString());
  }
  @Test public void unreadableImageDoesNotOverwriteExistingClipboard() throws Exception {
    result(ImageTextExtractor.clean(ImageTextExtractor.NO_TEXT));
    assertEquals("Keep existing clipboard", clipboard.getPrimaryClip().getItemAt(0).getText().toString());
    assertFalse(((android.widget.Button)field("copy")).isEnabled());
  }
  @Test public void backgroundResultWaitsForVisibleActivityBeforeCopying() throws Exception {
    invoke("onPause", new Class<?>[]{}); result("Recognized text");
    assertEquals("Keep existing clipboard", clipboard.getPrimaryClip().getItemAt(0).getText().toString());
    invoke("onResume", new Class<?>[]{});
    assertEquals("Recognized text", clipboard.getPrimaryClip().getItemAt(0).getText().toString());
  }
  @Test public void galleryCancellationLeavesClipboardUntouched() throws Exception {
    GeminiAiService.setApiKey(activity, "fixture-key");
    invoke("chooseGallery", new Class<?>[]{});
    Intent picker = Shadows.shadowOf(activity).getNextStartedActivityForResult().intent;
    assertEquals(Intent.ACTION_GET_CONTENT, picker.getAction()); assertEquals("image/*", picker.getType());
    invoke("onActivityResult", new Class<?>[]{int.class, int.class, Intent.class}, 102, android.app.Activity.RESULT_CANCELED, null);
    assertEquals("Keep existing clipboard", clipboard.getPrimaryClip().getItemAt(0).getText().toString());
  }
  @org.robolectric.annotation.Implements(androidx.core.content.FileProvider.class)
  public static class CameraProvider {
    @org.robolectric.annotation.Implementation protected static Uri getUriForFile(Context context, String authority, File file) {
      return Uri.parse("content://" + authority + "/ocr_camera/" + file.getName());
    }
  }
  @Test @Config(sdk = 34, shadows = CameraProvider.class)
  public void cameraReceivesPrivateFullResolutionOutputAndCancellationDeletesIt() throws Exception {
    GeminiAiService.setApiKey(activity, "fixture-key");
    invoke("chooseCamera", new Class<?>[]{});
    Intent capture = Shadows.shadowOf(activity).getNextStartedActivityForResult().intent;
    assertEquals(MediaStore.ACTION_IMAGE_CAPTURE, capture.getAction());
    Uri output = capture.getParcelableExtra(MediaStore.EXTRA_OUTPUT); assertNotNull(output);
    assertEquals("content", output.getScheme()); assertNotNull(capture.getClipData());
    assertTrue((capture.getFlags() & Intent.FLAG_GRANT_WRITE_URI_PERMISSION) != 0);
    File photo = (File)field("photo"); assertTrue(photo.isFile());
    invoke("onActivityResult", new Class<?>[]{int.class, int.class, Intent.class}, 101, android.app.Activity.RESULT_CANCELED, null);
    assertFalse(photo.exists());
  }
  @Test public void finishedActivityRejectsLateCallbacks() throws Exception {
    activity.finish(); assertEquals(false, invoke("current", new Class<?>[]{int.class}, 0));
  }
}
