package typodev.keyboard.clipboard;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enhanced data model for a clipboard entry with smart categorization (URL, Email, Phone, Image)
 * and deep entity extraction for 1-tap contextual quick actions.
 */
public class ClipboardItem
{
  public enum Category
  {
    URL("Link"),
    EMAIL("Email"),
    PHONE("Phone"),
    IMAGE("Image"),
    TEXT("Text");

    public final String label;
    Category(String label)
    {
      this.label = label;
    }
  }

  private static final Pattern URL_PATTERN = Pattern.compile(
      "\\b(?:https?://|www\\.)\\S+\\b", Pattern.CASE_INSENSITIVE);
  private static final Pattern EMAIL_PATTERN = Pattern.compile(
      "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");
  private static final Pattern PHONE_PATTERN = Pattern.compile(
      "(?:\\+?\\d{1,3}[-\\s]?)?\\(?\\d{3,4}\\)?[-\\s]?\\d{3,4}[-\\s]?\\d{3,4}");

  public final String content;
  public final long timestamp;
  public boolean isPinned;
  public final Category category;

  // Extracted entities for 1-tap smart quick actions
  public final String extractedUrl;
  public final String extractedPhone;
  public final String extractedEmail;
  public final boolean isImage;
  public final String imageUri;

  public ClipboardItem(String content, boolean isPinned)
  {
    this(content, System.currentTimeMillis(), isPinned, false, null);
  }

  public ClipboardItem(String content, long timestamp, boolean isPinned)
  {
    this(content, timestamp, isPinned, false, null);
  }

  public ClipboardItem(String content, long timestamp, boolean isPinned, boolean isImage, String imageUri)
  {
    this.content = content != null ? content : "";
    this.timestamp = timestamp;
    this.isPinned = isPinned;
    this.isImage = isImage;
    this.imageUri = imageUri;

    if (isImage)
    {
      this.category = Category.IMAGE;
      this.extractedUrl = null;
      this.extractedPhone = null;
      this.extractedEmail = null;
    }
    else
    {
      this.category = detectCategory(this.content);
      this.extractedUrl = extractUrl(this.content);
      this.extractedPhone = extractPhone(this.content);
      this.extractedEmail = extractEmail(this.content);
    }
  }

  /** Stable identity for history deletion and selection, including image clips. */
  public String historyKey()
  {
    return isImage && imageUri != null ? imageUri : content;
  }

  public boolean hasUrl()
  {
    return extractedUrl != null && !extractedUrl.isEmpty();
  }

  public boolean hasPhone()
  {
    return extractedPhone != null && !extractedPhone.isEmpty();
  }

  public boolean hasEmail()
  {
    return extractedEmail != null && !extractedEmail.isEmpty();
  }

  public static Category detectCategory(String text)
  {
    if (text == null || text.trim().isEmpty()) return Category.TEXT;
    String trimmed = text.trim();

    // Check URL
    if (URL_PATTERN.matcher(trimmed).find())
    {
      return Category.URL;
    }

    // Check Email
    if (EMAIL_PATTERN.matcher(trimmed).find())
    {
      return Category.EMAIL;
    }

    // Check Phone number
    if (trimmed.length() >= 7 && trimmed.length() <= 16 && PHONE_PATTERN.matcher(trimmed).matches())
    {
      return Category.PHONE;
    }

    return Category.TEXT;
  }

  public static String extractUrl(String text)
  {
    if (text == null) return null;
    Matcher m = URL_PATTERN.matcher(text);
    if (m.find())
    {
      String u = m.group();
      if (!u.startsWith("http://") && !u.startsWith("https://"))
      {
        u = "https://" + u;
      }
      return u;
    }
    return null;
  }

  public static String extractPhone(String text)
  {
    if (text == null) return null;
    Matcher m = PHONE_PATTERN.matcher(text);
    if (m.find())
    {
      String raw = m.group().replaceAll("[\\s\\-()]", "");
      if (raw.length() >= 7 && raw.length() <= 15)
      {
        return raw;
      }
    }
    return null;
  }

  public static String extractEmail(String text)
  {
    if (text == null) return null;
    Matcher m = EMAIL_PATTERN.matcher(text);
    if (m.find())
    {
      return m.group();
    }
    return null;
  }
}
