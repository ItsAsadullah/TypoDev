package typodev.keyboard.clipboard;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/** Hides clips immediately while leaving their data intact during the undo window. */
public final class ClipboardPendingDeletion {
  public static final long UNDO_MS = 3000;
  private final Set<String> keys = new LinkedHashSet<>();
  private long deadline;
  public void addAll(Collection<String> added, long now) {
    keys.addAll(added); deadline = now + UNDO_MS;
  }
  public boolean contains(String key) { return keys.contains(key); }
  public int size() { return keys.size(); }
  public void undo() { keys.clear(); }
  public void undo(String key) { keys.remove(key); }
  public Set<String> takeExpired(long now) {
    Set<String> expired = new LinkedHashSet<>();
    if (now >= deadline) { expired.addAll(keys); keys.clear(); }
    return expired;
  }
}
