package typodev.keyboard.clipboard;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ClipboardPaneInteractionTest {
  private Activity activity;
  private ClipboardPaneView pane;

  @Before public void setup() throws Exception {
    Field imageSingleton = ClipboardImageStore.class.getDeclaredField("instance"); imageSingleton.setAccessible(true);
    imageSingleton.set(null, null);
    Field providerCache = androidx.core.content.FileProvider.class.getDeclaredField("sCache"); providerCache.setAccessible(true);
    ((java.util.Map<?, ?>)providerCache.get(null)).clear();
    activity = Robolectric.buildActivity(Activity.class).setup().get();
    PinnedClipboardStore store = PinnedClipboardStore.instance(activity);
    store.clearPinned(); store.pinClip("first clip"); store.pinClip("second clip");
    pane = new ClipboardPaneView(activity);
    activity.setContentView(pane);
    layout();
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    layout();
  }
  private void layout() {
    pane.measure(View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(450, View.MeasureSpec.EXACTLY));
    pane.layout(0, 0, 600, 450);
  }
  private Object field(String name) throws Exception {
    Field field = ClipboardPaneView.class.getDeclaredField(name); field.setAccessible(true);
    return field.get(pane);
  }
  @SuppressWarnings("unchecked") private Map<String, View> cards() throws Exception {
    return (Map<String, View>)field("_cards");
  }
  private void touch(int action, float x, float y) {
    MotionEvent event = MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), action, x, y, 0);
    pane.dispatchTouchEvent(event); event.recycle();
  }
  private void checkTree(View view) {
    if (view instanceof ViewGroup) {
      ViewGroup group = (ViewGroup)view;
      for (int i = 0; i < group.getChildCount(); i++) {
        assertNotNull("Child array must remain consistent after CANCEL", group.getChildAt(i));
        checkTree(group.getChildAt(i));
      }
    }
  }
  @Test public void repeatedDragReleaseAndCancelKeepDrawingTreeIntact() throws Exception {
    for (int i = 0; i < 12; i++) {
      View card = cards().values().iterator().next();
      int[] point = new int[2], origin = new int[2];
      card.getLocationOnScreen(point); pane.getLocationOnScreen(origin);
      float x = point[0] - origin[0] + 12, y = point[1] - origin[1] + 12;
      touch(MotionEvent.ACTION_DOWN, x, y);
      Runnable hold = (Runnable)field("_activeHold"); assertNotNull(hold); hold.run();
      assertNotNull(field("_draggedClip"));
      touch(MotionEvent.ACTION_MOVE, x + 120, y + 30);
      touch(i % 2 == 0 ? MotionEvent.ACTION_UP : MotionEvent.ACTION_CANCEL, x + 120, y + 30);
      org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
      layout(); checkTree(pane); assertEquals(2, cards().size());
      pane.draw(new Canvas(Bitmap.createBitmap(600, 450, Bitmap.Config.ARGB_8888)));
    }
  }
  @Test public void rebuildingAnActiveTouchTargetDoesNotReenterGridMutation() throws Exception {
    View card = cards().values().iterator().next();
    int[] point = new int[2], origin = new int[2]; card.getLocationOnScreen(point); pane.getLocationOnScreen(origin);
    touch(MotionEvent.ACTION_DOWN, point[0] - origin[0] + 12, point[1] - origin[1] + 12);
    ((Runnable)field("_activeHold")).run();
    Method finish = ClipboardPaneView.class.getDeclaredMethod("finishPreview"); finish.setAccessible(true); finish.invoke(pane);
    pane.refreshCards(); checkTree(pane); assertEquals(2, cards().size());
    pane.draw(new Canvas(Bitmap.createBitmap(600, 450, Bitmap.Config.ARGB_8888)));
  }
  @Test public void cardMenuHasPreviewAndScrollableActionsAboveNavigationArea() throws Exception {
    Method menu = ClipboardPaneView.class.getDeclaredMethod("showCardContextMenu", ClipboardItem.class, View.class);
    menu.setAccessible(true); menu.invoke(pane, new ClipboardItem("second clip", true), cards().get("second clip"));
    layout(); ViewGroup panel = (ViewGroup)field("_menuPanel");
    assertEquals("Menu must use the existing toolbar, with no second header", 1, panel.getChildCount());
    assertEquals(View.GONE, ((View)field("_btnAddClip")).getVisibility());
    assertEquals("Close menu", ((View)field("_btnBackup")).getContentDescription().toString());
    ((View)field("_btnBackup")).performClick();
    assertEquals(View.GONE, panel.getVisibility());
    assertEquals(View.VISIBLE, ((View)field("_btnAddClip")).getVisibility());
    assertEquals("Clipboard options", ((View)field("_btnBackup")).getContentDescription().toString());
    menu.invoke(pane, new ClipboardItem("second clip", true), cards().get("second clip")); layout();
    ViewGroup content = (ViewGroup)panel.getChildAt(0);
    assertEquals(2, content.getChildCount());
    assertTrue(content.getChildAt(0).getWidth() > 0);
    ScrollView actions = (ScrollView)content.getChildAt(1);
    assertTrue(actions.getPaddingBottom() >= 48);
    actions.fullScroll(View.FOCUS_DOWN);
    ViewGroup list = (ViewGroup)actions.getChildAt(0);
    TextView last = (TextView)list.getChildAt(list.getChildCount() - 1);
    assertEquals("Delete", last.getText().toString());
    actions.scrollTo(0, list.getHeight());
    assertTrue("Delete must scroll fully above the reserved navigation area",
        last.getBottom() - actions.getScrollY() <= actions.getHeight() - actions.getPaddingBottom());
    assertTrue(last.getTop() - actions.getScrollY() >= 0);
    last.performClick();
    assertEquals(1, ((ClipboardPendingDeletion)field("_pendingDeletes")).size());
  }
  @Test public void undoReplacesTheCardAtTheSameHeightAndRestoresItsPin() throws Exception {
    View original = cards().get("second clip"); int height = original.getHeight();
    Method delete = ClipboardPaneView.class.getDeclaredMethod("queueDelete", String.class); delete.setAccessible(true);
    delete.invoke(pane, "second clip"); layout();
    View placeholder = cards().get("second clip");
    assertTrue(placeholder instanceof TextView); assertEquals(height, placeholder.getHeight());
    assertEquals(View.GONE, ((View)field("_undoBar")).getVisibility());
    placeholder.performClick(); layout();
    assertEquals(0, ((ClipboardPendingDeletion)field("_pendingDeletes")).size());
    assertTrue(PinnedClipboardStore.instance(activity).isPinned("second clip"));
    assertEquals(height, cards().get("second clip").getHeight());
    delete.invoke(pane, "second clip");
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(3000));
    assertFalse(PinnedClipboardStore.instance(activity).isPinned("second clip"));
    assertFalse(cards().containsKey("second clip"));
    checkTree(pane);
  }

  @Test public void searchSelectionReplacesAndDeletesOnlySelectedQueryText() throws Exception {
    ClipboardSearchBarView search = new ClipboardSearchBarView(activity);
    activity.setContentView(search); search.setQuery("first second"); search.focusQuery();
    Field queryField = ClipboardSearchBarView.class.getDeclaredField("_etSearch"); queryField.setAccessible(true);
    android.widget.EditText query = (android.widget.EditText)queryField.get(search);
    assertTrue(query.isFocused()); assertTrue(query.isCursorVisible()); assertTrue(query.isLongClickable());
    assertFalse(query.getShowSoftInputOnFocus());
    android.view.inputmethod.InputConnection connection = search.createInputConnection();
    assertTrue(connection.setSelection(6, 12)); assertEquals("second", connection.getSelectedText(0).toString());
    connection.commitText("word", 1); assertEquals("first word", query.getText().toString());
    connection.setSelection(0, 5); connection.deleteSurroundingText(1, 0);
    assertEquals(" word", query.getText().toString());
  }

  @Test public void searchIntentsEncodeTextAndGrantReadAccessToOnlyTheSharedImage() {
    String text = "বাংলা & Arabic + query?";
    android.content.Intent web = ClipboardSearchActions.webIntent(text);
    assertEquals(text, web.getData().getQueryParameter("q"));
    android.net.Uri image = android.net.Uri.parse("content://typodev.keyboard.debug.clipboard.images/clipboard_images/test.png");
    android.content.Intent lens = ClipboardSearchActions.imageIntent(image, "image/png", "com.google.ar.lens");
    assertEquals(android.content.Intent.ACTION_SEND, lens.getAction());
    assertEquals("com.google.ar.lens", lens.getPackage());
    assertEquals(image, lens.getParcelableExtra(android.content.Intent.EXTRA_STREAM));
    assertEquals(image, lens.getClipData().getItemAt(0).getUri());
    assertTrue((lens.getFlags() & android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0);
    assertEquals(0, lens.getFlags() & android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
  }
  private boolean containsLabel(View view, String label) {
    if (view instanceof TextView && label.equals(((TextView)view).getText().toString())) return true;
    if (view instanceof ViewGroup) {
      ViewGroup group = (ViewGroup)view;
      for (int i = 0; i < group.getChildCount(); i++) if (containsLabel(group.getChildAt(i), label)) return true;
    }
    return false;
  }

  @Test public void imageMenuOffersMagicCommentsWhileTextKeepsItsOwnAiActions() throws Exception {
    Method menu = ClipboardPaneView.class.getDeclaredMethod("showCardContextMenu", ClipboardItem.class, View.class);
    menu.setAccessible(true);
    String label = activity.getString(typodev.keyboard.R.string.clipboard_ai_image_comments);
    ClipboardItem image = new ClipboardItem("[Image]", System.currentTimeMillis(), false, true,
        "content://typodev.keyboard.debug.clipboard.images/clipboard_images/image.jpg");
    menu.invoke(pane, image, null);
    View panel = (View)field("_menuPanel");
    assertTrue(containsLabel(panel, label));
    assertTrue(containsLabel(panel, activity.getString(typodev.keyboard.R.string.clipboard_visual_search)));
    assertFalse(containsLabel(panel, "Grammar Fix & Polish"));
    menu.invoke(pane, new ClipboardItem("second clip", true), null);
    assertFalse(containsLabel(panel, label));
    assertTrue(containsLabel(panel, "AI assistant"));
  }

  private void awaitImageWorker(ClipboardImageStore store) throws Exception {
    Field worker = ClipboardImageStore.class.getDeclaredField("worker"); worker.setAccessible(true);
    ((java.util.concurrent.ExecutorService)worker.get(store)).submit(() -> {}).get(10, java.util.concurrent.TimeUnit.SECONDS);
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
  }

  // Android FileProvider uses slash paths; a Windows host needs a file URI for this decode fixture.
  @org.robolectric.annotation.Implements(androidx.core.content.FileProvider.class)
  public static class ImageFileProviderShadow {
    @org.robolectric.annotation.Implementation
    protected static android.net.Uri getUriForFile(android.content.Context context, String authority, java.io.File file) {
      return android.net.Uri.fromFile(file);
    }
  }

  @Test @Config(sdk = 34, shadows = ImageFileProviderShadow.class)
  public void analysisReadsSelectedSavedImageInsteadOfLatestScreenshot() throws Exception {
    ClipboardImageStore store = ClipboardImageStore.get(activity);
    store.clearRecent();
    String[] encodedImages = {"iVBORw0KGgoAAAANSUhEUgAAAFAAAAAoCAIAAADmAupWAAAAIElEQVR4nO3BMQEAAADCoPVPbQo/oAAAAAAAAAAAAOhoJagAAV+lx34AAAAASUVORK5CYII=", "iVBORw0KGgoAAAANSUhEUgAAAKAAAAAoCAIAAAD2TmbPAAAAKklEQVR4nO3BMQEAAADCoPVPbQ0PoAAAAAAAAAAAAAAAAAAAAAAAAKA7A0soAAE/tGlGAAAAAElFTkSuQmCC"};
    Field directoryField = ClipboardImageStore.class.getDeclaredField("directory"); directoryField.setAccessible(true);
    Field entriesField = ClipboardImageStore.class.getDeclaredField("entries"); entriesField.setAccessible(true);
    @SuppressWarnings("unchecked") java.util.List<org.json.JSONObject> entries =
        (java.util.List<org.json.JSONObject>)entriesField.get(store);
    for (int i = 0; i < encodedImages.length; i++) {
      String name = (i == 0 ? "a" : "b").repeat(64) + ".png";
      java.nio.file.Files.write(new java.io.File((java.io.File)directoryField.get(store), name).toPath(),
          java.util.Base64.getDecoder().decode(encodedImages[i]));
      org.json.JSONObject entry = new org.json.JSONObject(); entry.put("file", name);
      entry.put("time", System.currentTimeMillis()); entry.put("pinned", false); entry.put("mime", "image/png");
      entries.add(0, entry);
    }
    java.util.List<ClipboardItem> images = store.items(); assertEquals(2, images.size());
    String firstKey = images.get(1).historyKey();
    final byte[][] result = new byte[1][];
    store.readForAnalysis(firstKey, jpeg -> result[0] = jpeg); awaitImageWorker(store);
    assertNotNull(result[0]); assertTrue(result[0].length > 0);
    Bitmap decoded = android.graphics.BitmapFactory.decodeByteArray(result[0], 0, result[0].length);
    assertEquals(80, decoded.getWidth()); assertEquals(40, decoded.getHeight());
    store.delete(firstKey);
    store.readForAnalysis(firstKey, jpeg -> result[0] = jpeg); awaitImageWorker(store);
    assertNull("Deleted clips must not silently fall back to another image", result[0]);
  }

}
