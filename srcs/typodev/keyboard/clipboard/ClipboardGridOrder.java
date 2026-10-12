package typodev.keyboard.clipboard;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;

/** Persisted ordering independent of card dimensions and pin state. */
public final class ClipboardGridOrder {
  private final List<String> keys = new ArrayList<>();
  public ClipboardGridOrder(String json) {
    try {
      JSONArray array = new JSONArray(json);
      for (int i = 0; i < array.length(); i++) {
        Object value = array.opt(i);
        if (value instanceof String && !keys.contains(value)) keys.add((String)value);
      }
    } catch (Exception ignored) { }
  }
  public List<String> reconcile(List<String> available) {
    keys.retainAll(available);
    List<String> added = new ArrayList<>();
    for (String key : available) if (!keys.contains(key) && !added.contains(key)) added.add(key);
    keys.addAll(0, added);
    return new ArrayList<>(keys);
  }
  public boolean move(String source, String target, boolean after) {
    if (source.equals(target) || !keys.contains(source) || !keys.contains(target)) return false;
    keys.remove(source); keys.add(keys.indexOf(target) + (after ? 1 : 0), source); return true;
  }
  public boolean moveInto(String source, String target) {
    return move(source, target, keys.indexOf(source) < keys.indexOf(target));
  }
  public boolean moveToEnd(String source) {
    if (!keys.remove(source)) return false;
    keys.add(source); return true;
  }
  public String serialize() { return new JSONArray(keys).toString(); }
}
