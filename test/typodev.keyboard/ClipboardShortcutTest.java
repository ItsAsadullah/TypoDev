package typodev.keyboard;

import org.junit.Test;
import static org.junit.Assert.*;
import typodev.keyboard.clipboard.PinnedClipboardStore;
import typodev.keyboard.suggestions.Candidate;

public class ClipboardShortcutTest {
  @Test public void savingPinsAndFindsExactTextAcrossLanguages() {
    PinnedClipboardStore store = new PinnedClipboardStore(null);
    String text = "আল্লাহ আমাদের কল্যাণ করুন।\nالسلام عليكم";
    assertTrue(store.setShortcut(text, " SALAAM "));
    assertTrue(store.isPinned(text));
    assertEquals("salaam", store.getShortcut(text));
    assertEquals(text, store.queryShortcutCandidates("SALAAM", 6).get(0).word);
    assertEquals(Candidate.Source.SNIPPET, store.queryShortcutCandidates("sal", 6).get(0).source);
    assertTrue(store.queryShortcutCandidates("s", 6).isEmpty());
  }
  @Test public void collisionsAndInvalidShortcutsDoNotReplaceSavedText() {
    PinnedClipboardStore store = new PinnedClipboardStore(null);
    assertTrue(store.setShortcut("First clip", "greet"));
    assertFalse(store.setShortcut("Second clip", "GREET"));
    assertFalse(store.setShortcut("Second clip", "two words"));
    assertFalse(store.setShortcut("Second clip", "x"));
    assertFalse(store.setShortcut("Second clip", "|||"));
    assertEquals("First clip", store.queryShortcutCandidates("greet", 6).get(0).word);
    assertFalse(store.isPinned("Second clip"));
  }
  @Test public void backupRestorePreservesShortcutsAndOldBackupsWork() {
    PinnedClipboardStore original = new PinnedClipboardStore(null);
    original.setShortcut("Address\nDhaka", "address");
    PinnedClipboardStore restored = new PinnedClipboardStore(null);
    assertEquals(1, restored.importFromJson(original.exportToJson()));
    assertEquals("address", restored.getShortcut("Address\nDhaka"));
    assertEquals("Address\nDhaka", restored.queryShortcutCandidates("address", 6).get(0).word);
    assertEquals(1, restored.importFromJson("{\"version\":1,\"pinned\":[\"Legacy clip\"]}"));
    assertTrue(restored.isPinned("Legacy clip"));
  }
  @Test public void editingRemovingAndUnpinningCleanMappings() {
    PinnedClipboardStore store = new PinnedClipboardStore(null);
    store.setShortcut("Saved", "oldkey");
    store.setShortcut("Saved", "newkey");
    assertTrue(store.queryShortcutCandidates("oldkey", 6).isEmpty());
    assertTrue(store.setShortcut("Saved", ""));
    assertTrue(store.isPinned("Saved"));
    assertTrue(store.queryShortcutCandidates("newkey", 6).isEmpty());
    store.setShortcut("Saved", "again"); store.unpinClip("Saved");
    assertTrue(store.queryShortcutCandidates("again", 6).isEmpty());
  }
  @Test public void unicodePrefixesAndExactFirstAreSupported() {
    PinnedClipboardStore store = new PinnedClipboardStore(null);
    assertTrue(store.setShortcut("Greeting", "سلام"));
    assertEquals("Greeting", store.queryShortcutCandidates("سلا", 6).get(0).word);
    store.setShortcut("Long", "greetmore"); store.setShortcut("Exact", "greet");
    assertEquals("Exact", store.queryShortcutCandidates("greet", 1).get(0).word);
    assertTrue(store.queryShortcutCandidates(null, 3).isEmpty());
    assertTrue(store.queryShortcutCandidates("greet", 0).isEmpty());
  }
}
