package typodev.keyboard.autofill;

import android.app.assist.AssistStructure;
import android.view.View;
import android.text.InputType;
import android.util.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Use field metadata and verification-screen context; numeric input alone is not OTP evidence. */
@android.annotation.TargetApi(28)
public final class OtpFieldDetector {
  private OtpFieldDetector() {}
  public static AssistStructure.ViewNode find(AssistStructure structure) {
    List<AssistStructure.ViewNode> fields = findFields(structure);
    return fields.isEmpty() ? null : fields.get(0);
  }
  public static List<AssistStructure.ViewNode> findFields(AssistStructure structure) {
    for (int i = 0; i < structure.getWindowNodeCount(); i++) {
      List<AssistStructure.ViewNode> result = findFields(structure.getWindowNodeAt(i).getRootViewNode());
      if (!result.isEmpty()) return result;
    }
    return new ArrayList<>();
  }
  static AssistStructure.ViewNode find(AssistStructure.ViewNode root) {
    List<AssistStructure.ViewNode> fields = findFields(root);
    return fields.isEmpty() ? null : fields.get(0);
  }
  static List<AssistStructure.ViewNode> findFields(AssistStructure.ViewNode root) {
    List<AssistStructure.ViewNode> candidates = new ArrayList<>();
    boolean context = screenHasOtpContext(root);
    collect(root, context, candidates);
    List<AssistStructure.ViewNode> digits = new ArrayList<>();
    collectDigitFields(root, context, digits);
    if (digits.size() >= 4 && digits.size() <= 8)
      for (AssistStructure.ViewNode digit : digits) if (digit.isFocused()) return digits;
    for (AssistStructure.ViewNode candidate : candidates) if (candidate.isFocused())
      return java.util.Collections.singletonList(candidate);
    // Some virtual views omit focus metadata. Only infer a target when it is unambiguous.
    return candidates.size() == 1 ? candidates : new ArrayList<>();
  }
  private static void collectDigitFields(AssistStructure.ViewNode node, boolean context,
      List<AssistStructure.ViewNode> digits) {
    if (context && node.getAutofillId() != null && node.getAutofillType() == View.AUTOFILL_TYPE_TEXT
        && node.getVisibility() == View.VISIBLE && node.isEnabled() && maxLength(node) == 1
        && (isOtp(node) || contextualCodeField(node, true, true))) {
      String[] hints = node.getAutofillHints();
      if (hints == null || hints.length == 0 || isOtp(node)) digits.add(node);
    }
    for (int i = 0; i < node.getChildCount(); i++) collectDigitFields(node.getChildAt(i), context, digits);
  }
  private static void collect(AssistStructure.ViewNode node, boolean otpContext,
      List<AssistStructure.ViewNode> candidates) {
    if (node.getAutofillId() != null && node.getAutofillType() == View.AUTOFILL_TYPE_TEXT
        && node.getVisibility() == View.VISIBLE && node.isEnabled()
        && (isOtp(node) || contextualCodeField(node, otpContext))) candidates.add(node);
    for (int i = 0; i < node.getChildCount(); i++) {
      collect(node.getChildAt(i), otpContext, candidates);
    }
  }
  private static boolean screenHasOtpContext(AssistStructure.ViewNode node) {
    if (node.getVisibility() != View.VISIBLE) return false;
    if (node.getAutofillType() != View.AUTOFILL_TYPE_TEXT
        && (hasOtpLabel(string(node.getText())) || hasOtpLabel(string(node.getContentDescription())))) return true;
    for (int i = 0; i < node.getChildCount(); i++) if (screenHasOtpContext(node.getChildAt(i))) return true;
    return false;
  }
  private static boolean contextualCodeField(AssistStructure.ViewNode node, boolean context) {
    return contextualCodeField(node, context, false);
  }
  private static boolean contextualCodeField(AssistStructure.ViewNode node, boolean context, boolean digit) {
    if (!context) return false;
    String label = normalize(node.getIdEntry()) + " " + normalize(node.getHint()) + " "
        + normalize(string(node.getContentDescription()));
    if (label.matches(".*\\b(password|pin|phone|mobile|amount|postal|zip|coupon|promo|card|account)\\b.*")) return false;
    String[] hints = node.getAutofillHints();
    if (hints != null && hints.length > 0) return false; // Respect another declared field purpose.
    int inputClass = node.getInputType() & InputType.TYPE_MASK_CLASS;
    int variation = node.getInputType() & InputType.TYPE_MASK_VARIATION;
    if (inputClass == InputType.TYPE_CLASS_TEXT && (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD
        || variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)) return false;
    boolean bounded = maxLength(node) >= 4 && maxLength(node) <= 10;
    return label.matches(".*\\bcode\\b.*") || label.contains("কোড")
        || (inputClass == InputType.TYPE_CLASS_NUMBER && bounded)
        || (digit && maxLength(node) == 1
            && (inputClass == InputType.TYPE_CLASS_NUMBER || inputClass == InputType.TYPE_CLASS_TEXT
                || inputClass == InputType.TYPE_CLASS_PHONE));
  }
  private static int maxLength(AssistStructure.ViewNode node) {
    if (node.getMaxTextLength() > 0) return node.getMaxTextLength();
    android.view.ViewStructure.HtmlInfo html = node.getHtmlInfo();
    if (html != null && html.getAttributes() != null) for (Pair<String, String> attr : html.getAttributes())
      if ("maxlength".equalsIgnoreCase(attr.first)) {
        try { return Integer.parseInt(attr.second); }
        catch (NumberFormatException invalid) { return -1; }
      }
    return -1;
  }
  private static boolean isOtp(AssistStructure.ViewNode node) {
    String[] hints = node.getAutofillHints();
    if (hints != null) for (String hint : hints)
      if ("smsOTPCode".equalsIgnoreCase(hint)) return true;
    if (hasOtpLabel(node.getIdEntry()) || hasOtpLabel(node.getHint())
        || hasOtpLabel(string(node.getContentDescription()))) return true;
    android.view.ViewStructure.HtmlInfo html = node.getHtmlInfo();
    if (html != null && html.getAttributes() != null) for (Pair<String, String> attr : html.getAttributes()) {
      if ("autocomplete".equalsIgnoreCase(attr.first) && "one-time-code".equalsIgnoreCase(attr.second)) return true;
      if (("name".equalsIgnoreCase(attr.first) || "id".equalsIgnoreCase(attr.first)
          || "placeholder".equalsIgnoreCase(attr.first)) && hasOtpLabel(attr.second)) return true;
    }
    return false;
  }
  private static String string(CharSequence text) { return text == null ? null : text.toString(); }
  private static String normalize(String label) {
    return label == null ? "" : label.replaceAll("([A-Z]+)([A-Z][a-z])", "$1 $2")
        .replaceAll("([a-z0-9])([A-Z])", "$1 $2").toLowerCase(Locale.ROOT).replace('_', ' ').replace('-', ' ');
  }
  static boolean hasOtpLabel(String label) {
    if (label == null) return false;
    String value = normalize(label);
    return value.matches(".*\\botp\\b.*") || value.contains("verification code")
        || value.contains("verificationcode") || value.contains("sms code")
        || value.contains("smscode") || value.contains("one time code")
        || value.contains("onetimecode") || value.contains("ওটিপি") || value.contains("যাচাইকরণ কোড")
        || value.contains("ভেরিফিকেশন কোড") || value.contains("যাচাই কোড");
  }
}
