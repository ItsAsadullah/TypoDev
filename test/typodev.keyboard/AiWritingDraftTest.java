package typodev.keyboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import typodev.keyboard.ai.AiWritingDraft;
import typodev.keyboard.ai.AiWritingResultsView;

public class AiWritingDraftTest {
  private AiWritingDraft draft(List<String> results, byte[] image, int index) {
    return new AiWritingDraft("আগের লেখা", image, "রিফ্রেজ", AiWritingResultsView.Task.REWRITE,
        results, index, "আগের লেখা", "শেষ", "", "example.editor", 12);
  }

  @Test public void closingAndClearingLiveResultsDoesNotEraseTheDraft() {
    List<String> live = new ArrayList<>(Arrays.asList("এক", "দুই", "তিন"));
    AiWritingDraft saved = draft(live, null, 1);
    live.clear();
    assertEquals(Arrays.asList("এক", "দুই", "তিন"), saved.results);
    assertEquals(1, saved.index);
    assertEquals("রিফ্রেজ", saved.tone);
    assertEquals("আগের লেখা", saved.source);
    assertEquals(AiWritingResultsView.Task.REWRITE, saved.task);
  }

  @Test public void onlyTheOriginalUnchangedTextFieldCanReceiveTheRestoredDraft() {
    AiWritingDraft saved = draft(Arrays.asList("ফল"), null, 0);
    assertTrue(saved.matchesEditor("example.editor", 12, "আগের লেখা", "শেষ", ""));
    assertFalse(saved.matchesEditor("another.editor", 12, "আগের লেখা", "শেষ", ""));
    assertFalse(saved.matchesEditor("example.editor", 13, "আগের লেখা", "শেষ", ""));
    assertFalse(saved.matchesEditor("example.editor", 12, "বদলে গেছে", "শেষ", ""));
    assertFalse(saved.matchesEditor("example.editor", 12, "আগের লেখা", "", ""));
    assertFalse(saved.matchesEditor("example.editor", 12, "আগের লেখা", "শেষ", "সিলেকশন"));
    assertFalse(saved.matchesEditor(null, 12, "আগের লেখা", "শেষ", ""));
  }

  @Test public void retainedImageCannotBeChangedByTheCaller() {
    byte[] image = {1, 2};
    AiWritingDraft saved = draft(Arrays.asList("ফল"), image, 9);
    image[0] = 3;
    byte[] returned = saved.getImage();
    assertEquals(1, returned[0]);
    returned[0] = 4;
    assertEquals(1, saved.getImage()[0]);
    assertEquals(0, saved.index);
  }
}
