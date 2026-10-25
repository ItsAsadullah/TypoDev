package typodev.keyboard;

import org.junit.Test;
import typodev.keyboard.clipboard.ClipboardItem;
import typodev.keyboard.clipboard.PinnedClipboardStore;
import static org.junit.Assert.*;

public class ClipboardSuiteTest
{
  @Test
  public void testUrlCategoryDetection()
  {
    assertEquals(ClipboardItem.Category.URL, ClipboardItem.detectCategory("https://github.com/google/gemini"));
    assertEquals(ClipboardItem.Category.URL, ClipboardItem.detectCategory("Visit www.techhat.org for updates"));
  }

  @Test
  public void testEmailCategoryDetection()
  {
    assertEquals(ClipboardItem.Category.EMAIL, ClipboardItem.detectCategory("developer@typodev.keyboard.com"));
    assertEquals(ClipboardItem.Category.EMAIL, ClipboardItem.detectCategory("Contact us at support@example.org"));
  }

  @Test
  public void testOtpCodeCategoryDetection()
  {
    assertEquals(ClipboardItem.Category.CODE_OTP, ClipboardItem.detectCategory("492810"));
    assertEquals(ClipboardItem.Category.CODE_OTP, ClipboardItem.detectCategory("1234"));
    assertEquals(ClipboardItem.Category.CODE_OTP, ClipboardItem.detectCategory("Your verification code is 882910"));
  }

  @Test
  public void testPhoneCategoryDetection()
  {
    assertEquals(ClipboardItem.Category.PHONE, ClipboardItem.detectCategory("+8801712345678"));
    assertEquals(ClipboardItem.Category.PHONE, ClipboardItem.detectCategory("01812345678"));
  }

  @Test
  public void testPlainTextCategory()
  {
    assertEquals(ClipboardItem.Category.TEXT, ClipboardItem.detectCategory("Hello, this is a normal clipboard message."));
  }

  @Test
  public void testOtpExtraction()
  {
    assertEquals("748291", ClipboardItem.extractOtp("748291"));
    assertEquals("492019", ClipboardItem.extractOtp("Your bKash verification code is 492019. Do not share."));
    assertEquals("582104", ClipboardItem.extractOtp("আপনার ওটিপি কোড 582104"));
    assertNull(ClipboardItem.extractOtp("Just random text with no code"));
  }

  @Test
  public void testUrlExtraction()
  {
    assertEquals("https://github.com/google/gemini", ClipboardItem.extractUrl("Check out https://github.com/google/gemini now"));
    assertEquals("https://www.techhat.org", ClipboardItem.extractUrl("Go to www.techhat.org"));
    assertNull(ClipboardItem.extractUrl("No url in this sentence"));
  }

  @Test
  public void testPhoneExtraction()
  {
    assertEquals("+8801712345678", ClipboardItem.extractPhone("Call me at +8801712345678 tomorrow"));
    assertEquals("01812345678", ClipboardItem.extractPhone("My phone is 01812345678"));
  }

  @Test
  public void testEmailExtraction()
  {
    assertEquals("support@typodev.com", ClipboardItem.extractEmail("Send email to support@typodev.com"));
    assertNull(ClipboardItem.extractEmail("No email address"));
  }

  @Test
  public void testImageItemCategory()
  {
    ClipboardItem img = new ClipboardItem("[Image]", System.currentTimeMillis(), false, true, "content://media/external/images/media/42");
    assertTrue(img.isImage);
    assertEquals(ClipboardItem.Category.IMAGE, img.category);
    assertEquals("content://media/external/images/media/42", img.imageUri);
  }

  @Test
  public void testRecentUnpastedTracking()
  {
    ClipboardHistoryService.setRecentCopiedClip("https://flutter.dev");
    assertEquals("https://flutter.dev", ClipboardHistoryService.getRecentUnpastedClip());

    ClipboardHistoryService.markRecentClipPasted();
    assertNull(ClipboardHistoryService.getRecentUnpastedClip());

    ClipboardHistoryService.setRecentCopiedClip("Another clip");
    assertEquals("Another clip", ClipboardHistoryService.getRecentUnpastedClip());

    ClipboardHistoryService.dismissRecentClip();
    assertNull(ClipboardHistoryService.getRecentUnpastedClip());
  }

  @Test
  public void testPinnedStoreInMemory()
  {
    PinnedClipboardStore store = new PinnedClipboardStore(null);
    store.clearPinned();

    store.pinClip("My Bkash Number 01700000000");
    assertTrue(store.isPinned("My Bkash Number 01700000000"));
    assertEquals(1, store.getPinnedClips().size());

    store.unpinClip("My Bkash Number 01700000000");
    assertFalse(store.isPinned("My Bkash Number 01700000000"));
    assertEquals(0, store.getPinnedClips().size());
  }

  @Test
  public void testPinnedStoreExportAndImportJson()
  {
    PinnedClipboardStore store = new PinnedClipboardStore(null);
    store.clearPinned();

    store.pinClip("Bkash Pin 12345");
    store.pinClip("Important Address Bangladesh");
    assertEquals(2, store.getPinnedClips().size());

    String json = store.exportToJson();
    assertNotNull(json);
    assertTrue(json.contains("Bkash Pin 12345"));
    assertTrue(json.contains("Important Address Bangladesh"));

    store.clearPinned();
    assertEquals(0, store.getPinnedClips().size());

    int imported = store.importFromJson(json);
    assertEquals(2, imported);
    assertTrue(store.isPinned("Bkash Pin 12345"));
    assertTrue(store.isPinned("Important Address Bangladesh"));
  }
}
