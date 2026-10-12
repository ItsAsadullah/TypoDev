package typodev.keyboard.clipboard;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/** Storage-independent metadata codec and pin/expiry rules. */
final class ClipboardImageIndex {
  final List<JSONObject> entries = new ArrayList<>();
  ClipboardImageIndex(String json) {
    try {
      JSONArray array = new JSONArray(json);
      for (int i = 0; i < array.length(); i++) {
        JSONObject entry = array.optJSONObject(i);
        if (entry != null && validName(entry.optString("file")) && entry.optLong("time") > 0
            && find(entry.optString("file")) == null) entries.add(entry);
      }
    } catch (Exception ignored) { }
  }
  static boolean validName(String file) { return file.matches("[a-f0-9]{64}\\.(png|jpg|webp|gif)"); }
  String serialize() { return new JSONArray(entries).toString(); }
  JSONObject find(String file) {
    for (JSONObject entry : entries) if (file.equals(entry.optString("file"))) return entry;
    return null;
  }
  void pin(String file, boolean pinned, long now) {
    JSONObject entry = find(file);
    if (entry == null) return;
    try { entry.put("pinned", pinned); entry.put("time", now); } catch (Exception ignored) { }
  }
  static boolean expired(JSONObject entry, long now, long ttl) {
    return ttl >= 0 && !entry.optBoolean("pinned") && now - entry.optLong("time") > ttl;
  }
  void touch(String file, long now) {
    JSONObject entry = find(file);
    if (entry != null && !entry.optBoolean("pinned")) {
      try { entry.put("time", now); } catch (Exception ignored) { }
    }
  }
}
