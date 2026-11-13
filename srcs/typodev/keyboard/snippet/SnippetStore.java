package typodev.keyboard.snippet;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Singleton store for all user snippets.
 * Uses SharedPreferences with serialized Snippet objects — no SQLite required.
 */
public final class SnippetStore
{
  private static final String PREFS_NAME = "typodev_snippets";
  private static final String KEY_IDS = "snippet_ids";
  private static final String KEY_SNIPPET_PREFIX = "s_";
  private static final String KEY_SEEDED = "snippet_seeded_v1";

  private static SnippetStore _instance;

  private volatile Context _context;
  private SharedPreferences _prefs;

  private static boolean isDirectBootLocked(Context context)
  {
    if (context == null || android.os.Build.VERSION.SDK_INT < 24) return false;
    try
    {
      android.os.UserManager um = (android.os.UserManager) context.getSystemService(Context.USER_SERVICE);
      return um != null && !um.isUserUnlocked();
    }
    catch (Throwable ignored)
    {
      return false;
    }
  }

  private SnippetStore(Context context)
  {
    _context = context != null ? context.getApplicationContext() : null;
    ensurePrefs();
  }

  private synchronized SharedPreferences ensurePrefs()
  {
    if (_prefs != null) return _prefs;
    if (_context == null || isDirectBootLocked(_context)) return null;
    try
    {
      _prefs = _context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    catch (RuntimeException ignored) {}
    return _prefs;
  }

  public static synchronized SnippetStore instance(Context context)
  {
    if (_instance == null)
    {
      _instance = new SnippetStore(context);
    }
    else if (context != null)
    {
      if (_instance._context == null)
      {
        _instance._context = context.getApplicationContext();
      }
      _instance.ensurePrefs();
    }
    return _instance;
  }

  /** Persist (add or update) a snippet. */
  public synchronized void save(Snippet snippet)
  {
    if (snippet == null || snippet.shortcut.isEmpty()) return;
    SharedPreferences prefs = ensurePrefs();
    if (prefs == null) return;
    SharedPreferences.Editor ed = prefs.edit();

    Set<String> ids = new java.util.HashSet<>(prefs.getStringSet(KEY_IDS, new java.util.HashSet<String>()));
    ids.add(String.valueOf(snippet.id));
    ed.putStringSet(KEY_IDS, ids);
    ed.putString(KEY_SNIPPET_PREFIX + snippet.id, snippet.serialize());
    ed.apply();
  }

  /** Delete a snippet by id. */
  public synchronized void delete(long snippetId)
  {
    SharedPreferences prefs = ensurePrefs();
    if (prefs == null) return;
    SharedPreferences.Editor ed = prefs.edit();
    Set<String> ids = new java.util.HashSet<>(prefs.getStringSet(KEY_IDS, new java.util.HashSet<String>()));
    ids.remove(String.valueOf(snippetId));
    ed.putStringSet(KEY_IDS, ids);
    ed.remove(KEY_SNIPPET_PREFIX + snippetId);
    ed.apply();
  }

  /** Retrieve a snippet by ID. Returns null if not found. */
  public synchronized Snippet getById(long id)
  {
    SharedPreferences prefs = ensurePrefs();
    if (prefs == null) return null;
    String raw = prefs.getString(KEY_SNIPPET_PREFIX + id, null);
    return raw != null ? Snippet.deserialize(raw) : null;
  }

  public synchronized Snippet getById(String idStr)
  {
    if (idStr == null) return null;
    try
    {
      return getById(Long.parseLong(idStr));
    }
    catch (Throwable t)
    {
      return null;
    }
  }

  /** Return ALL snippets sorted by shortcut alphabetically. */
  public synchronized List<Snippet> getAll()
  {
    SharedPreferences prefs = ensurePrefs();
    if (prefs == null) return Collections.emptyList();
    Set<String> ids = prefs.getStringSet(KEY_IDS, Collections.<String>emptySet());
    List<Snippet> result = new ArrayList<>(ids.size());
    for (String idStr : ids)
    {
      String raw = prefs.getString(KEY_SNIPPET_PREFIX + idStr, null);
      Snippet s = Snippet.deserialize(raw);
      if (s != null) result.add(s);
    }
    Collections.sort(result, new Comparator<Snippet>()
    {
      @Override
      public int compare(Snippet a, Snippet b)
      {
        return a.shortcut.compareToIgnoreCase(b.shortcut);
      }
    });
    return result;
  }

  /**
   * Find snippets whose shortcut EXACTLY matches the typed word.
   */
  public synchronized List<Snippet> findByExactShortcut(String word)
  {
    if (word == null || word.isEmpty()) return Collections.emptyList();
    SharedPreferences prefs = ensurePrefs();
    if (prefs == null) return Collections.emptyList();
    String lower = word.trim().toLowerCase(Locale.ROOT);
    List<Snippet> result = new ArrayList<>();
    Set<String> ids = prefs.getStringSet(KEY_IDS, Collections.<String>emptySet());
    for (String idStr : ids)
    {
      String raw = prefs.getString(KEY_SNIPPET_PREFIX + idStr, null);
      Snippet s = Snippet.deserialize(raw);
      if (s != null && s.shortcut.equalsIgnoreCase(lower))
        result.add(s);
    }
    return result;
  }

  /**
   * Find snippets whose shortcut STARTS WITH the typed prefix.
   * Returns exact matches first, then prefix matches.
   */
  public synchronized List<Snippet> findByShortcutPrefix(String prefix)
  {
    if (prefix == null || prefix.isEmpty()) return Collections.emptyList();
    SharedPreferences prefs = ensurePrefs();
    if (prefs == null) return Collections.emptyList();
    String lower = prefix.trim().toLowerCase(Locale.ROOT);
    List<Snippet> exact = new ArrayList<>();
    List<Snippet> prefixMatches = new ArrayList<>();
    Set<String> ids = prefs.getStringSet(KEY_IDS, Collections.<String>emptySet());
    for (String idStr : ids)
    {
      String raw = prefs.getString(KEY_SNIPPET_PREFIX + idStr, null);
      Snippet s = Snippet.deserialize(raw);
      if (s == null) continue;
      if (s.shortcut.equalsIgnoreCase(lower))
        exact.add(s);
      else if (s.shortcut.startsWith(lower))
        prefixMatches.add(s);
    }
    List<Snippet> result = new ArrayList<>(exact.size() + prefixMatches.size());
    result.addAll(exact);
    result.addAll(prefixMatches);
    return result;
  }

  /**
   * Return snippets grouped by category (for the Snippet Manager UI).
   */
  public synchronized Map<SnippetCategory, List<Snippet>> getAllByCategory()
  {
    List<Snippet> all = getAll();
    Map<SnippetCategory, List<Snippet>> map = new LinkedHashMap<>();
    for (SnippetCategory cat : SnippetCategory.values())
      map.put(cat, new ArrayList<Snippet>());
    for (Snippet s : all)
    {
      List<Snippet> catList = map.get(s.category);
      if (catList != null) catList.add(s);
    }
    return map;
  }

  /** Total number of saved snippets. */
  public synchronized int count()
  {
    SharedPreferences prefs = ensurePrefs();
    if (prefs == null) return 0;
    return prefs.getStringSet(KEY_IDS, Collections.<String>emptySet()).size();
  }

  /**
   * Seeds a few example snippets on first run so users understand the feature
   * immediately without needing to add anything.
   */
  public synchronized void seedDefaults()
  {
    SharedPreferences prefs = ensurePrefs();
    if (prefs == null || prefs.getBoolean(KEY_SEEDED, false)) return;
    save(Snippet.create("greet",
        "Assalamu Alaikum. I hope you are doing well. How can I help you today?",
        "Greeting Message", SnippetCategory.CHAT));
    save(Snippet.create("thanks",
        "Thank you for reaching out. I really appreciate your message.",
        "Thank You", SnippetCategory.CHAT));
    save(Snippet.create("office",
        "Our office is open from 9:00 AM to 6:00 PM, Saturday to Thursday.",
        "Office Hours", SnippetCategory.BUSINESS));
    prefs.edit().putBoolean(KEY_SEEDED, true).apply();
  }
}
