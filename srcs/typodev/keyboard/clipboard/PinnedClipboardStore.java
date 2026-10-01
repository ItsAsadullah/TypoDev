package typodev.keyboard.clipboard;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * Storage manager for pinned clipboard entries, stored in SharedPreferences.
 */
public class PinnedClipboardStore
{
  private static final String PREF_FILE = "pinned_clipboards";
  private static final String KEY_PINNED = "pinned";

  private static PinnedClipboardStore _instance;

  public static synchronized PinnedClipboardStore instance(Context context)
  {
    if (_instance == null)
    {
      _instance = new PinnedClipboardStore(context != null ? context.getApplicationContext() : null);
    }
    return _instance;
  }

  private final Context _context;
  private final List<String> _pinnedList = new ArrayList<>();
  private boolean _loaded;

  public PinnedClipboardStore(Context context)
  {
    _context = context;
    load();
  }

  public synchronized List<String> getPinnedClips()
  {
    ensureLoaded();
    return new ArrayList<>(_pinnedList);
  }

  public synchronized boolean isPinned(String text)
  {
    ensureLoaded();
    if (text == null) return false;
    return _pinnedList.contains(text);
  }

  public synchronized void pinClip(String text)
  {
    if (!ensureLoaded()) return;
    if (text == null || text.trim().isEmpty()) return;
    _pinnedList.remove(text);
    _pinnedList.add(0, text); // Most recent pinned first
    save();
  }

  public synchronized void unpinClip(String text)
  {
    if (!ensureLoaded()) return;
    if (text == null) return;
    if (_pinnedList.remove(text))
    {
      save();
    }
  }

  public synchronized void clearPinned()
  {
    if (!ensureLoaded()) return;
    _pinnedList.clear();
    save();
  }

  public synchronized String exportToJson()
  {
    ensureLoaded();
    try
    {
      org.json.JSONObject root = new org.json.JSONObject();
      root.put("version", 1);
      root.put("timestamp", System.currentTimeMillis());
      JSONArray arr = new JSONArray();
      for (String item : _pinnedList)
      {
        if (item != null)
        {
          arr.put(item);
        }
      }
      root.put("pinned", arr);
      return root.toString(2);
    }
    catch (Exception e)
    {
      return "[]";
    }
  }

  public synchronized int importFromJson(String jsonString)
  {
    if (!ensureLoaded()) return 0;
    if (jsonString == null || jsonString.trim().isEmpty()) return 0;
    int imported = 0;
    try
    {
      JSONArray arr = null;
      String trimmed = jsonString.trim();
      if (trimmed.startsWith("{"))
      {
        org.json.JSONObject root = new org.json.JSONObject(trimmed);
        arr = root.optJSONArray("pinned");
      }
      else if (trimmed.startsWith("["))
      {
        arr = new JSONArray(trimmed);
      }

      if (arr != null)
      {
        for (int i = 0; i < arr.length(); i++)
        {
          Object value = arr.opt(i);
          if (!(value instanceof String)) continue;
          String s = (String)value;
          if (!s.trim().isEmpty() && !_pinnedList.contains(s))
          {
            _pinnedList.add(s);
            imported++;
          }
        }
        if (imported > 0)
        {
          save();
        }
      }
    }
    catch (Exception ignored) {}
    return imported;
  }

  private boolean ensureLoaded()
  {
    if (!_loaded) load();
    return _loaded;
  }

  private synchronized void load()
  {
    if (_context == null)
    {
      _loaded = true;
      return;
    }
    try
    {
      SharedPreferences prefs = _context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
      String raw = prefs.getString(KEY_PINNED, null);
      List<String> loaded = new ArrayList<>();
      if (raw != null && !raw.isEmpty())
      {
        JSONArray arr = new JSONArray(raw);
        for (int i = 0; i < arr.length(); i++)
        {
          Object value = arr.opt(i);
          if (value instanceof String && !((String)value).trim().isEmpty()
              && !loaded.contains(value))
            loaded.add((String)value);
        }
      }
      _pinnedList.clear();
      _pinnedList.addAll(loaded);
      _loaded = true;
    }
    catch (JSONException e) { _loaded = true; }
    catch (Exception ignored) {}
  }

  private synchronized void save()
  {
    if (_context == null) return;
    try
    {
      JSONArray arr = new JSONArray();
      for (String s : _pinnedList)
      {
        arr.put(s);
      }
      SharedPreferences prefs = _context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
      prefs.edit().putString(KEY_PINNED, arr.toString()).apply();
    }
    catch (Exception ignored) {}
  }
}
