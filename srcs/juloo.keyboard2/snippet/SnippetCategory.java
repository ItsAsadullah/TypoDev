package juloo.keyboard2.snippet;

/** Categories for organizing snippets. */
public enum SnippetCategory
{
  PERSONAL("👤", "Personal"),
  WORK("💼", "Work"),
  EMAIL("📧", "Email"),
  CONTACT("📱", "Contact"),
  ADDRESS("🏠", "Address"),
  WEBSITE("🌐", "Website"),
  CHAT("💬", "Chat"),
  SOCIAL("📣", "Social Media"),
  CODE("💻", "Code"),
  BUSINESS("🏢", "Business"),
  TEMPLATE("📝", "Templates");

  public final String emoji;
  public final String label;

  SnippetCategory(String emoji, String label)
  {
    this.emoji = emoji;
    this.label = label;
  }

  public String displayName()
  {
    return emoji + " " + label;
  }

  /** Returns the category for a stored string key, defaulting to PERSONAL. */
  public static SnippetCategory fromKey(String key)
  {
    if (key == null) return PERSONAL;
    try { return SnippetCategory.valueOf(key); }
    catch (IllegalArgumentException e) { return PERSONAL; }
  }
}
