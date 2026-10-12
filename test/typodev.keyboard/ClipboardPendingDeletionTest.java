package typodev.keyboard.clipboard;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Arrays;

public class ClipboardPendingDeletionTest {
  @Test public void deletionWaitsThreeSeconds() {
    ClipboardPendingDeletion pending = new ClipboardPendingDeletion();
    pending.addAll(Arrays.asList("text", "image-uri"), 100);
    assertTrue(pending.contains("text"));
    assertTrue(pending.takeExpired(3099).isEmpty());
    assertEquals(2, pending.takeExpired(3100).size());
    assertEquals(0, pending.size());
  }
  @Test public void undoProtectsBothTextAndImages() {
    ClipboardPendingDeletion pending = new ClipboardPendingDeletion();
    pending.addAll(Arrays.asList("pinned-shortcut", "image-uri"), 100);
    pending.undo();
    assertFalse(pending.contains("image-uri"));
    assertTrue(pending.takeExpired(5000).isEmpty());
  }
  @Test public void nextSwipeGetsFullUndoWindowForTheGroup() {
    ClipboardPendingDeletion pending = new ClipboardPendingDeletion();
    pending.addAll(Arrays.asList("one"), 0);
    pending.addAll(Arrays.asList("two"), 2000);
    assertTrue(pending.takeExpired(3000).isEmpty());
    assertEquals(2, pending.takeExpired(5000).size());
  }
  @Test public void undoOneClipLeavesOtherDeletionPending() {
    ClipboardPendingDeletion pending = new ClipboardPendingDeletion();
    pending.addAll(Arrays.asList("one", "two"), 0);
    pending.undo("one");
    assertFalse(pending.contains("one"));
    assertTrue(pending.contains("two"));
    assertEquals(java.util.Collections.singleton("two"), pending.takeExpired(3000));
  }
}
