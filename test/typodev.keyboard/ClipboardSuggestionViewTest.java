package typodev.keyboard;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.time.Duration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import typodev.keyboard.clipboard.ClipboardImageStore;
import typodev.keyboard.suggestions.CandidatesView;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk = 34)
public class ClipboardSuggestionViewTest {
  private Activity activity;
  private CandidatesView candidates;
  @Before public void setup() throws Exception {
    Field singleton = ClipboardImageStore.class.getDeclaredField("instance"); singleton.setAccessible(true); singleton.set(null, null);
    activity = Robolectric.buildActivity(Activity.class).setup().get();
    activity.setTheme(R.style.Dark);
    // Android's FileProvider uses Unix path separators; supply its URI boundary on the Windows test host.
    Field cache = androidx.core.content.FileProvider.class.getDeclaredField("sCache"); cache.setAccessible(true);
    java.util.Map providerCache = (java.util.Map)cache.get(null); providerCache.clear();
    Class<?> strategy = Class.forName("androidx.core.content.FileProvider$PathStrategy");
    providerCache.put(activity.getPackageName() + ".clipboard.images", java.lang.reflect.Proxy.newProxyInstance(
        strategy.getClassLoader(), new Class<?>[]{strategy}, (proxy, method, args) -> {
          if (method.getName().equals("getUriForFile")) return Uri.parse("content://" + activity.getPackageName()
              + ".clipboard.images/clipboard/" + ((File)args[0]).getName());
          return null;
        }));
    activity.getSharedPreferences("clipboard_images", Context.MODE_PRIVATE).edit().clear().commit();
    Config config = new Config(); config.clipboard_history_enabled = true; config.clipboard_history_duration = 30;
    Field global = Config.class.getDeclaredField("_globalConfig"); global.setAccessible(true); global.set(null, config);
    View root = LayoutInflater.from(activity).inflate(R.layout.keyboard, null);
    candidates = root.findViewById(R.id.candidates_view);
    ((android.view.ViewGroup)candidates.getParent()).removeView(candidates);
    activity.setContentView(candidates);
  }
  @org.junit.After public void cleanupProvider() throws Exception {
    Field cache = androidx.core.content.FileProvider.class.getDeclaredField("sCache"); cache.setAccessible(true);
    ((java.util.Map)cache.get(null)).clear();
  }
  @Test public void firstPreviewHidesToolbarAndExpiresToNormalSuggestions() {
    ClipboardHistoryService.setRecentCopiedClip("Preview text");
    candidates.updateClipboardSuggestionChip();
    assertEquals(View.VISIBLE, candidates.findViewById(R.id.clipboard_suggestion_container).getVisibility());
    assertEquals(View.GONE, candidates.findViewById(R.id.toolbar_scroll).getVisibility());
    assertEquals(View.GONE, candidates.findViewById(R.id.btn_toolbar_toggle).getVisibility());
    Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(8));
    assertEquals(View.GONE, candidates.findViewById(R.id.clipboard_suggestion_container).getVisibility());
    assertEquals(View.VISIBLE, candidates.findViewById(R.id.suggestions_scroll).getVisibility());
    assertEquals(View.VISIBLE, candidates.findViewById(R.id.btn_magic_autocomplete).getVisibility());
    assertEquals(View.VISIBLE, candidates.findViewById(R.id.btn_candidate_mic).getVisibility());
    candidates.updateClipboardSuggestionChip();
    assertEquals(View.GONE, candidates.findViewById(R.id.clipboard_suggestion_container).getVisibility());
  }
  @Test public void closingTextPreviewRestoresBothButtonIcons() {
    ClipboardHistoryService.setRecentCopiedClip("Dismiss this preview");
    candidates.updateClipboardSuggestionChip();
    candidates.findViewById(R.id.clipboard_suggestion_close).performClick();
    for (int id : new int[]{R.id.btn_magic_autocomplete_container, R.id.btn_magic_autocomplete,
        R.id.btn_candidate_mic_container, R.id.btn_candidate_mic})
      assertEquals(View.VISIBLE, candidates.findViewById(id).getVisibility());
  }
  @Test public void previewReceivedWhileKeyboardIsHiddenIsNotDismissedBeforeOpening() {
    candidates.setVisibility(View.GONE);
    ClipboardHistoryService.setRecentCopiedClip("Waiting for keyboard");
    candidates.updateClipboardSuggestionChip();
    Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(9));
    assertEquals("Waiting for keyboard", ClipboardHistoryService.getRecentUnpastedClip());
    candidates.setVisibility(View.VISIBLE);
    candidates.updateClipboardSuggestionChip();
    assertEquals(View.VISIBLE, candidates.findViewById(R.id.clipboard_suggestion_container).getVisibility());
  }
  @Test public void latestImageShowsThumbnailAndDismissalPersists() throws Exception {
    ClipboardHistoryService.setRecentCopiedClip("older text");
    ClipboardImageStore store = ClipboardImageStore.get(activity);
    // Seed an imported private image; this test checks presentation, not Android's bitmap codec.
    String name = String.format(java.util.Locale.ROOT, "%064d", 0) + ".png";
    File directory = new File(activity.getFilesDir(), "clipboard_images"); directory.mkdirs();
    byte[] png = java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=");
    for (String file : new String[]{name, name + ".png"})
      try (FileOutputStream output = new FileOutputStream(new File(directory, file))) { output.write(png); }
    long time = ClipboardHistoryService.recentTextTimestamp() + 1;
    Field entries = ClipboardImageStore.class.getDeclaredField("entries"); entries.setAccessible(true);
    ((java.util.List<org.json.JSONObject>)entries.get(store)).add(new org.json.JSONObject()
        .put("file", name).put("time", time).put("pinned", false));
    activity.getSharedPreferences("clipboard_images", Context.MODE_PRIVATE).edit()
        .putString("suggestion_file", name).putLong("suggestion_time", time).commit();
    candidates.updateClipboardSuggestionChip();
    assertEquals(View.VISIBLE, candidates.findViewById(R.id.clipboard_suggestion_container).getVisibility());
    assertNotNull(((android.widget.ImageView)candidates.findViewById(R.id.clipboard_suggestion_icon)).getDrawable());
    candidates.findViewById(R.id.clipboard_suggestion_close).performClick();
    assertNull(store.recentSuggestion(0));
    assertEquals(View.VISIBLE, candidates.findViewById(R.id.suggestions_scroll).getVisibility());
  }
}
