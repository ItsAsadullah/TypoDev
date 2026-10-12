package typodev.keyboard.clipboard;

import org.junit.Test;
import static org.junit.Assert.*;
import org.json.JSONObject;

public class ClipboardImageIndexTest {
  private static final String FILE = new String(new char[64]).replace('\0', 'a') + ".png";
  @Test public void copyingAnExistingScreenshotRenewsExpiryAndKeepsItsPin() throws Exception {
    ClipboardImageIndex index = new ClipboardImageIndex("[]");
    index.entries.add(new JSONObject().put("file", FILE).put("time", 1000));
    assertTrue(ClipboardImageIndex.expired(index.find(FILE), 100000, 30000));
    index.touch(FILE, 100000);
    assertFalse(ClipboardImageIndex.expired(index.find(FILE), 110000, 30000));
    index.pin(FILE, true, 100000); index.touch(FILE, 120000);
    assertTrue(index.find(FILE).getBoolean("pinned"));
    assertEquals(100000, index.find(FILE).getLong("time"));
    assertEquals(1, new ClipboardImageIndex(index.serialize()).entries.size());
  }
  @Test public void pinsSurviveReloadAndRecentExpiry() throws Exception {
    ClipboardImageIndex original = new ClipboardImageIndex("[]");
    original.entries.add(new JSONObject().put("file", FILE).put("time", 1000).put("mime", "image/png"));
    original.pin(FILE, true, 2000);
    ClipboardImageIndex reloaded = new ClipboardImageIndex(original.serialize());
    assertEquals(1, reloaded.entries.size());
    assertEquals("image/png", reloaded.find(FILE).getString("mime"));
    assertFalse(ClipboardImageIndex.expired(reloaded.find(FILE), 100000, 30000));
    reloaded.pin(FILE, false, 100000);
    assertFalse(ClipboardImageIndex.expired(reloaded.find(FILE), 110000, 30000));
    assertTrue(ClipboardImageIndex.expired(reloaded.find(FILE), 140000, 30000));
    assertFalse(ClipboardImageIndex.expired(reloaded.find(FILE), Long.MAX_VALUE, -1));
  }
  @Test public void corruptMetadataAndPathTraversalRejected() throws Exception {
    assertTrue(new ClipboardImageIndex("not json").entries.isEmpty());
    assertTrue(new ClipboardImageIndex("[{\"file\":\"../private.png\",\"time\":1}]").entries.isEmpty());
    JSONObject entry = new JSONObject().put("file", FILE).put("time", 1000);
    assertEquals(1, new ClipboardImageIndex("[" + entry + "," + entry + "]").entries.size());
  }
  @Test public void screenshotDetectionDoesNotImportOrdinaryPhotos() {
    assertTrue(ClipboardImageStore.isScreenshot("Pictures/Screenshots/Screenshot_20261007.png"));
    assertTrue(ClipboardImageStore.isScreenshot("Screen_shot_123.jpg"));
    assertTrue(ClipboardImageStore.isScreenshot("Screen-capture-123.jpg"));
    assertFalse(ClipboardImageStore.isScreenshot("DCIM/Camera/IMG_123.jpg"));
  }
}
