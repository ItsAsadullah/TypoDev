package typodev.keyboard;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;
import typodev.keyboard.clipboard.ClipboardGridOrder;

public class ClipboardGridOrderTest {
  @Test public void movesAcrossPositionsAndSurvivesReload() {
    ClipboardGridOrder order = new ClipboardGridOrder("[]");
    order.reconcile(Arrays.asList("A", "B", "C", "D"));
    assertTrue(order.move("D", "A", false));
    assertEquals(Arrays.asList("D", "A", "B", "C"), order.reconcile(Arrays.asList("A", "B", "C", "D")));
    assertTrue(order.move("A", "C", true));
    ClipboardGridOrder restored = new ClipboardGridOrder(order.serialize());
    assertEquals(Arrays.asList("D", "B", "C", "A"), restored.reconcile(Arrays.asList("A", "B", "C", "D")));
  }
  @Test public void newClipsAppearFirstAndExpiredClipsDisappear() {
    ClipboardGridOrder order = new ClipboardGridOrder("[\"B\",\"A\"]");
    assertEquals(Arrays.asList("NEW", "B", "A"), order.reconcile(Arrays.asList("NEW", "A", "B")));
    assertEquals(Arrays.asList("NEW", "A"), order.reconcile(Arrays.asList("A", "NEW")));
    assertTrue(order.moveToEnd("NEW"));
    assertEquals(Arrays.asList("A", "NEW"), order.reconcile(Arrays.asList("A", "NEW")));
  }
  @Test public void invalidDropsAndDuplicateMetadataAreSafe() {
    ClipboardGridOrder order = new ClipboardGridOrder("[\"A\",\"A\",12,\"B\"]");
    assertEquals(Arrays.asList("A", "B"), order.reconcile(Arrays.asList("A", "B")));
    assertFalse(order.move("A", "A", false));
    assertFalse(order.move("missing", "B", true));
    assertFalse(order.move("A", "missing", false));
    assertTrue(order.moveInto("A", "B"));
    assertEquals(Arrays.asList("B", "A"), order.reconcile(Arrays.asList("A", "B")));
    assertTrue(order.moveInto("A", "B"));
    assertEquals(Arrays.asList("A", "B"), order.reconcile(Arrays.asList("A", "B")));
  }
}
