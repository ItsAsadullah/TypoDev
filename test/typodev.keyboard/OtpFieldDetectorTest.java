package typodev.keyboard.autofill;

import android.app.assist.AssistStructure;
import android.text.InputType;
import android.view.View;
import android.view.ViewStructure;
import android.view.autofill.AutofillId;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class OtpFieldDetectorTest {
  private ViewStructure builder() throws Exception {
    AssistStructure.ViewNode node = ReflectionHelpers.callConstructor(AssistStructure.ViewNode.class);
    Class<?> type = Class.forName("android.app.assist.AssistStructure$ViewNodeBuilder");
    return (ViewStructure)ReflectionHelpers.callConstructor(type,
        ReflectionHelpers.ClassParameter.from(AssistStructure.class, null),
        ReflectionHelpers.ClassParameter.from(AssistStructure.ViewNode.class, node),
        ReflectionHelpers.ClassParameter.from(boolean.class, false));
  }
  private AssistStructure.ViewNode node(ViewStructure builder) { return ReflectionHelpers.getField(builder, "mNode"); }
  private void editable(ViewStructure field, int id, String hint) {
    field.setAutofillId(ReflectionHelpers.callConstructor(AutofillId.class,
        ReflectionHelpers.ClassParameter.from(int.class, id)));
    field.setAutofillType(View.AUTOFILL_TYPE_TEXT); field.setHint(hint); field.setEnabled(true);
  }
  @Test public void camelCaseOtpIdsAndAccessibilityLabelsAreRecognized() throws Exception {
    ViewStructure field = builder(); editable(field, 10, null); field.setFocused(true);
    field.setId(10, "app", "id", "otpInput");
    assertSame(node(field), OtpFieldDetector.find(node(field)));
    field.setId(10, "app", "id", "input"); field.setContentDescription("SMS verification code");
    assertSame(node(field), OtpFieldDetector.find(node(field)));
    assertTrue(OtpFieldDetector.hasOtpLabel("OTPInput"));
  }
  @Test public void genericCodeFieldNeedsVerificationHeading() throws Exception {
    ViewStructure root = builder(); root.setChildCount(2);
    ViewStructure heading = root.newChild(0); heading.setText("Enter SMS verification code");
    ViewStructure field = root.newChild(1); editable(field, 12, "Enter code"); field.setFocused(true);
    assertSame(node(field), OtpFieldDetector.find(node(root)));
    heading.setText("Enter discount code"); assertNull(OtpFieldDetector.find(node(root)));
  }
  @Test public void boundedNumericInputIsNotEnoughWithoutOtpContext() throws Exception {
    ViewStructure root = builder(); root.setChildCount(2);
    ViewStructure heading = root.newChild(0); heading.setText("Phone verification code");
    ViewStructure field = root.newChild(1); editable(field, 13, null);
    field.setInputType(InputType.TYPE_CLASS_NUMBER); field.setMaxTextLength(6); field.setFocused(true);
    assertSame(node(field), OtpFieldDetector.find(node(root)));
    heading.setText("Payment amount"); assertNull(OtpFieldDetector.find(node(root)));
    heading.setText("Phone verification code"); field.setHint("Phone number");
    assertNull(OtpFieldDetector.find(node(root)));
  }
  @Test public void declaredPasswordPurposeOverridesPageContext() throws Exception {
    ViewStructure root = builder(); root.setChildCount(2);
    root.newChild(0).setText("Verification code");
    ViewStructure field = root.newChild(1); editable(field, 14, "Enter code");
    field.setAutofillHints(new String[]{"password"}); field.setFocused(true);
    assertNull(OtpFieldDetector.find(node(root)));
  }
  @Test public void htmlOneTimeCodeMetadataWorksWithoutAndroidHints() throws Exception {
    ViewStructure field = builder(); editable(field, 15, null);
    ViewStructure.HtmlInfo.Builder html = field.newHtmlInfoBuilder("input");
    html.addAttribute("autocomplete", "one-time-code"); field.setHtmlInfo(html.build());
    assertSame(node(field), OtpFieldDetector.find(node(field)));
  }
  @Test public void missingFocusRequiresOneUnambiguousField() throws Exception {
    ViewStructure root = builder(); root.setChildCount(2);
    ViewStructure first = root.newChild(0); editable(first, 16, "OTP");
    ViewStructure second = root.newChild(1); editable(second, 17, "OTP");
    assertNull(OtpFieldDetector.find(node(root)));
    second.setFocused(true); assertSame(node(second), OtpFieldDetector.find(node(root)));
    second.setVisibility(View.GONE); assertSame(node(first), OtpFieldDetector.find(node(root)));
  }
  @Test public void sixDigitWebFormRequiresOtpContextAndFocusedDigit() throws Exception {
    ViewStructure root = builder(); root.setChildCount(7);
    ViewStructure heading = root.newChild(0); heading.setText("ভেরিফিকেশন কোড লিখুন");
    for (int i = 1; i <= 6; i++) {
      ViewStructure digit = root.newChild(i); editable(digit, 20 + i, null);
      digit.setInputType(InputType.TYPE_CLASS_PHONE); digit.setFocused(i == 1);
      ViewStructure.HtmlInfo.Builder html = digit.newHtmlInfoBuilder("input");
      html.addAttribute("maxlength", "1"); digit.setHtmlInfo(html.build());
    }
    assertEquals(6, OtpFieldDetector.findFields(node(root)).size());
    heading.setText("Card number"); assertTrue(OtpFieldDetector.findFields(node(root)).isEmpty());
  }
}
