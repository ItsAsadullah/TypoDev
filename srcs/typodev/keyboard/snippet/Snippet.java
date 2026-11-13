package typodev.keyboard.snippet;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a single user-defined snippet.
 * A snippet maps a short trigger (shortcut) to a longer expansion text.
 *
 * Example:
 *   shortcut  = "email"
 *   expansion = "asad@example.com"
 *   name      = "My Email"
 *   category  = EMAIL
 */
public final class Snippet
{
  private static final AtomicLong LAST_ID = new AtomicLong(0L);

  /** Unique identifier (monotonic timestamp at creation). */
  public final long id;
  /** Short trigger word the user types (e.g., "email", "greet"). */
  public String shortcut;
  /** Full text to expand to (e.g., "asad@example.com"). */
  public String expansion;
  /** Human-readable display name (e.g., "My Email Address"). */
  public String name;
  /** Organizational category. */
  public SnippetCategory category;

  public Snippet(long id, String shortcut, String expansion, String name, SnippetCategory category)
  {
    this.id = id;
    this.shortcut = (shortcut != null) ? shortcut.trim().toLowerCase(Locale.ROOT) : "";
    this.expansion = (expansion != null) ? expansion : "";
    this.name = (name != null && !name.isEmpty()) ? name : shortcut;
    this.category = (category != null) ? category : SnippetCategory.PERSONAL;
  }

  private static long nextId()
  {
    long now = System.currentTimeMillis();
    while (true)
    {
      long prev = LAST_ID.get();
      long next = Math.max(now, prev + 1L);
      if (LAST_ID.compareAndSet(prev, next))
      {
        return next;
      }
    }
  }

  /** Create a new snippet with strictly unique auto-generated id. */
  public static Snippet create(String shortcut, String expansion, String name, SnippetCategory category)
  {
    return new Snippet(nextId(), shortcut, expansion, name, category);
  }

  /**
   * Serialize to a pipe-delimited string for SharedPreferences storage.
   * Format: "id|shortcut|category|name|expansion"
   * (expansion last because it may contain any characters except our delimiter trick)
   */
  public String serialize()
  {
    String encodedExpansion = android.util.Base64.encodeToString(
        expansion.getBytes(java.nio.charset.StandardCharsets.UTF_8),
        android.util.Base64.NO_WRAP);
    String encodedName = android.util.Base64.encodeToString(
        name.getBytes(java.nio.charset.StandardCharsets.UTF_8),
        android.util.Base64.NO_WRAP);
    return id + "|" + shortcut + "|" + category.name() + "|" + encodedName + "|" + encodedExpansion;
  }

  /** Deserialize from the pipe-delimited format created by {@link #serialize()}. */
  public static Snippet deserialize(String raw)
  {
    if (raw == null || raw.isEmpty()) return null;
    try
    {
      String[] parts = raw.split("\\|", 5);
      if (parts.length < 5) return null;
      long id = Long.parseLong(parts[0]);
      String shortcut = parts[1];
      SnippetCategory cat = SnippetCategory.fromKey(parts[2]);
      String name = new String(android.util.Base64.decode(parts[3], android.util.Base64.NO_WRAP),
          java.nio.charset.StandardCharsets.UTF_8);
      String expansion = new String(android.util.Base64.decode(parts[4], android.util.Base64.NO_WRAP),
          java.nio.charset.StandardCharsets.UTF_8);
      return new Snippet(id, shortcut, expansion, name, cat);
    }
    catch (Throwable t)
    {
      return null;
    }
  }

  /** Returns a truncated preview of the expansion for display in the suggestion bar. */
  public String expansionPreview(int maxChars)
  {
    if (expansion.length() <= maxChars) return expansion;
    return expansion.substring(0, maxChars - 1) + "…";
  }
}
