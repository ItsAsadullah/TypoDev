package typodev.keyboard;

import android.content.SharedPreferences;
import java.lang.reflect.Proxy;
import org.junit.Test;
import static org.junit.Assert.*;
import typodev.keyboard.ai.SavedNoteStore;

public class SavedNoteStoreTest {
  private String data = "[]", pending;
  private boolean writable = true;
  private SavedNoteStore store() {
    SharedPreferences.Editor editor = (SharedPreferences.Editor)Proxy.newProxyInstance(
        SharedPreferences.Editor.class.getClassLoader(), new Class<?>[] {SharedPreferences.Editor.class}, (p, method, args) -> {
          if (method.getName().equals("putString")) { pending = (String)args[1]; return p; }
          if (method.getName().equals("commit")) { if (writable) data = pending; return writable; }
          throw new AssertionError("Unexpected preferences edit: " + method.getName());
        });
    SharedPreferences prefs = (SharedPreferences)Proxy.newProxyInstance(SharedPreferences.class.getClassLoader(),
        new Class<?>[] {SharedPreferences.class}, (p, method, args) -> {
          if (method.getName().equals("getString")) return data;
          if (method.getName().equals("edit")) return editor;
          throw new AssertionError("Unexpected preferences call: " + method.getName());
        });
    return new SavedNoteStore(prefs);
  }
  @Test public void notesPersistAcrossStoreInstancesInNewestFirstOrder() {
    store().save(" প্রথম নোট "); store().save("Meeting at 10");
    assertEquals(2, store().list().size());
    assertEquals("Meeting at 10", store().list().get(0).text);
    assertEquals("প্রথম নোট", store().list().get(1).text);
    assertTrue(store().list().get(0).savedAt > 0);
  }
  @Test public void explicitDuplicateMovesToFrontWithoutCreatingAnotherNote() {
    store().save("one"); String id = store().list().get(0).id;
    store().save("two"); store().save("one");
    assertEquals(2, store().list().size()); assertEquals(id, store().list().get(0).id);
  }
  @Test public void deletionAndClearRemoveSavedData() {
    store().save("one"); store().save("two");
    store().delete(store().list().get(1).id);
    assertEquals(1, store().list().size()); assertEquals("two", store().list().get(0).text);
    store().clear(); assertTrue(store().list().isEmpty());
  }
  @Test public void fullStoreNeverSilentlyDropsAnExistingNote() {
    for (int i = 0; i < SavedNoteStore.MAX_NOTES; i++) store().save("note " + i);
    String before = data;
    try { store().save("extra"); fail("Full store must reject new notes"); } catch (IllegalStateException expected) {}
    assertEquals(before, data);
    store().save("note 0"); assertEquals(SavedNoteStore.MAX_NOTES, store().list().size());
  }
  @Test public void malformedDataIsNotOverwrittenBySave() {
    data = "broken JSON";
    try { store().save("new note"); fail(); } catch (IllegalStateException expected) {}
    assertEquals("broken JSON", data);
  }
  @Test public void failedDiskWriteIsReported() {
    writable = false;
    try { store().save("note"); fail(); } catch (IllegalStateException expected) {}
    assertEquals("[]", data);
  }
  @Test(expected = IllegalArgumentException.class) public void blankNoteIsRejected() { store().save(" \n "); }
}
