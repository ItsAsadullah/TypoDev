package typodev.keyboard;

import java.lang.reflect.Field;
import java.util.List;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;
import typodev.keyboard.clipboard.ClipboardItem;

public class ClipboardHistoryRegressionTest
{
  private Field globalField;
  private Config previousConfig;
  private ClipboardHistoryService history;

  @Before public void setup() throws Exception
  {
    globalField = Config.class.getDeclaredField("_globalConfig");
    globalField.setAccessible(true);
    previousConfig = (Config)globalField.get(null);
    Config config = new Config();
    config.clipboard_history_enabled = true;
    config.clipboard_history_duration = 30;
    globalField.set(null, config);
    history = new ClipboardHistoryService(null);
  }

  @After public void restore() throws Exception
  {
    globalField.set(null, previousConfig);
  }

  @Test public void deletingOldEntryPreservesRecentCopy()
  {
    history.add_clip("old copy");
    history.add_clip("new copy");
    history.remove_history_entry("old copy");
    assertEquals("new copy", ClipboardHistoryService.getRecentUnpastedClip());
    assertEquals(1, history.clear_expired_and_get_history().size());
    history.remove_history_entry("new copy");
    assertNull(ClipboardHistoryService.getRecentUnpastedClip());
  }

  @Test public void imageDuplicatesAreSkippedAndDeletionUsesUri()
  {
    history.add_image_clip("content://images/1");
    history.add_image_clip("content://images/1");
    history.add_image_clip("content://images/2");
    List<ClipboardItem> items = history.clear_expired_and_get_history_items();
    assertEquals(2, items.size());
    assertNotEquals(items.get(0).historyKey(), items.get(1).historyKey());
    history.remove_history_entry(items.get(0).historyKey());
    items = history.clear_expired_and_get_history_items();
    assertEquals(1, items.size());
    assertEquals("content://images/1", items.get(0).imageUri);
  }

  @Test public void nullAndDisabledCopiesAreIgnored()
  {
    history.add_clip(null);
    history.add_clip("");
    Config.globalConfig().clipboard_history_enabled = false;
    history.add_clip("disabled");
    history.add_image_clip("content://images/1");
    assertTrue(history.clear_expired_and_get_history_items().isEmpty());
  }

  @Test public void clipboardCallbackBeforeConfigIsSafe() throws Exception
  {
    globalField.set(null, null);
    history.add_clip("early copy");
    history.add_image_clip("content://images/1");
    assertTrue(history.clear_expired_and_get_history_items().isEmpty());
  }
}
