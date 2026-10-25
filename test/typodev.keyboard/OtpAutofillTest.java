package typodev.keyboard.autofill;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.provider.Settings;
import android.view.autofill.AutofillManager;
import android.service.autofill.Dataset;
import com.google.android.gms.auth.api.phone.SmsCodeAutofillClient;
import com.google.android.gms.auth.api.phone.SmsCodeRetriever;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.api.internal.ApiKey;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import java.time.Duration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class OtpAutofillTest {
  static boolean ongoing;
  static int permission, starts;
  static int consentRequests;
  static Exception retrievalError;
  public static class TestActivity extends OtpAutofillActivity {
    @Override protected SmsCodeAutofillClient createClient() {
      return new SmsCodeAutofillClient() {
        public Task<Integer> checkPermissionState() { return Tasks.forResult(permission); }
        public Task<Boolean> hasOngoingSmsRequest(String packageName) { return Tasks.forResult(ongoing); }
        public Task<Void> startSmsCodeRetriever() {
          starts++;
          return retrievalError == null ? Tasks.forResult(null) : Tasks.forException(retrievalError);
        }
        public ApiKey<Api.ApiOptions.NoOptions> getApiKey() { return null; }
      };
    }
  }
  @Before public void setup() {
    Context context = RuntimeEnvironment.getApplication();
    Settings.Secure.putString(context.getContentResolver(), "autofill_service",
        new ComponentName(context, TypoDevAutofillService.class).flattenToString());
    ongoing = false; permission = SmsCodeAutofillClient.PermissionState.GRANTED; starts = 0;
    consentRequests = 0; retrievalError = null;
  }
  private ActivityController<TestActivity> open() {
    Context context = RuntimeEnvironment.getApplication();
    Intent intent = new Intent(context, OtpAutofillActivity.class)
        .putExtra(OtpAutofillActivity.EXTRA_FIELD, org.robolectric.util.ReflectionHelpers.callConstructor(
            android.view.autofill.AutofillId.class,
            org.robolectric.util.ReflectionHelpers.ClassParameter.from(int.class, 42)))
        .putExtra(OtpAutofillActivity.EXTRA_PACKAGE, "example.login");
    ActivityController<TestActivity> controller = Robolectric.buildActivity(TestActivity.class, intent).setup();
    Shadows.shadowOf(Looper.getMainLooper()).idle();
    return controller;
  }
  private void receive(OtpAutofillActivity activity, String code) {
    Intent broadcast = new Intent(SmsCodeRetriever.SMS_CODE_RETRIEVED_ACTION)
        .putExtra(SmsCodeRetriever.EXTRA_STATUS, new Status(CommonStatusCodes.SUCCESS))
        .putExtra(SmsCodeRetriever.EXTRA_SMS_CODE, code);
    // Deliver to the registered receiver directly; Android's sender permission is checked by the platform.
    for (org.robolectric.shadows.ShadowApplication.Wrapper wrapper :
        Shadows.shadowOf(RuntimeEnvironment.getApplication()).getRegisteredReceivers()) {
      if (wrapper.intentFilter.hasAction(SmsCodeRetriever.SMS_CODE_RETRIEVED_ACTION))
        wrapper.broadcastReceiver.onReceive(activity, broadcast);
    }
  }
  @Test public void fillsExactFieldWithoutCopyingOrPersistingCode() throws Exception {
    ActivityController<TestActivity> controller = open();
    assertEquals(1, starts);
    OtpAutofillActivity activity = controller.get(); receive(activity, "012345");
    assertEquals(Activity.RESULT_OK, Shadows.shadowOf(activity).getResultCode());
    Dataset dataset = Shadows.shadowOf(activity).getResultIntent().getParcelableExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT);
    java.util.List<?> values = (java.util.List<?>)Dataset.class.getDeclaredMethod("getFieldValues").invoke(dataset);
    java.util.List<?> ids = (java.util.List<?>)Dataset.class.getDeclaredMethod("getFieldIds").invoke(dataset);
    assertEquals("012345", ((android.view.autofill.AutofillValue)values.get(0)).getTextValue().toString());
    assertEquals(activity.getIntent().getParcelableExtra(OtpAutofillActivity.EXTRA_FIELD), ids.get(0));
    assertFalse(((android.content.ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE)).hasPrimaryClip());
    controller.pause().stop().destroy();
  }
  @Test public void deniedAndOngoingRequestsNeverStartRetrieval() {
    permission = SmsCodeAutofillClient.PermissionState.DENIED;
    ActivityController<TestActivity> first = open(); assertTrue(first.get().isFinishing()); assertEquals(0, starts);
    first.pause().stop().destroy();
    permission = SmsCodeAutofillClient.PermissionState.GRANTED; ongoing = true;
    ActivityController<TestActivity> second = open(); assertTrue(second.get().isFinishing()); assertEquals(0, starts);
    second.pause().stop().destroy();
  }
  @Test public void invalidCodeAndTimeoutReturnCanceled() {
    ActivityController<TestActivity> first = open(); receive(first.get(), "123456\nother text");
    assertEquals(Activity.RESULT_CANCELED, Shadows.shadowOf(first.get()).getResultCode());
    first.pause().stop().destroy();
    ActivityController<TestActivity> second = open();
    Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMinutes(5));
    assertTrue(second.get().isFinishing());
    assertEquals(Activity.RESULT_CANCELED, Shadows.shadowOf(second.get()).getResultCode());
    second.pause().stop().destroy();
  }
  @Test public void switchedProviderRejectsReceivedCode() {
    ActivityController<TestActivity> controller = open();
    Settings.Secure.putString(controller.get().getContentResolver(), "autofill_service", "other.provider/.Service");
    receive(controller.get(), "123456");
    assertEquals(Activity.RESULT_CANCELED, Shadows.shadowOf(controller.get()).getResultCode());
    controller.pause().stop().destroy();
  }
  private void requireConsent() {
    permission = SmsCodeAutofillClient.PermissionState.NONE;
    retrievalError = new com.google.android.gms.common.api.ResolvableApiException(new Status(6)) {
      @Override public void startResolutionForResult(Activity activity, int requestCode) {
        consentRequests++; assertEquals(6109, requestCode);
      }
    };
  }
  @Test public void firstUseConsentRetriesAndReturnsCode() {
    requireConsent();
    ActivityController<TestActivity> controller = open();
    assertEquals(1, consentRequests); assertFalse(controller.get().isFinishing());
    permission = SmsCodeAutofillClient.PermissionState.GRANTED; retrievalError = null;
    controller.get().onActivityResult(6109, Activity.RESULT_OK, null);
    Shadows.shadowOf(Looper.getMainLooper()).idle(); assertEquals(2, starts);
    receive(controller.get(), "0a12B3");
    assertEquals(Activity.RESULT_OK, Shadows.shadowOf(controller.get()).getResultCode());
    controller.pause().stop().destroy();
  }
  @Test public void canceledConsentDoesNotFill() {
    requireConsent(); ActivityController<TestActivity> controller = open();
    assertEquals(1, consentRequests);
    controller.get().onActivityResult(6109, Activity.RESULT_CANCELED, null);
    receive(controller.get(), "123456");
    assertTrue(controller.get().isFinishing());
    assertEquals(Activity.RESULT_CANCELED, Shadows.shadowOf(controller.get()).getResultCode());
    controller.pause().stop().destroy();
  }
  @Test public void otpRecognitionDoesNotTreatEveryCodeOrPinAsOtp() {
    assertTrue(OtpFieldDetector.hasOtpLabel("sms_otp"));
    assertTrue(OtpFieldDetector.hasOtpLabel("verificationCode"));
    assertTrue(OtpFieldDetector.hasOtpLabel("যাচাইকরণ কোড"));
    assertFalse(OtpFieldDetector.hasOtpLabel("password"));
    assertFalse(OtpFieldDetector.hasOtpLabel("PIN"));
    assertFalse(OtpFieldDetector.hasOtpLabel("postal_code"));
    assertFalse(OtpFieldDetector.hasOtpLabel("hotpot"));
    assertTrue(OtpAutofillActivity.validCode("0a12B3"));
    assertFalse(OtpAutofillActivity.validCode("12"));
  }
  @Test public void splitOtpReturnsOneDigitPerFieldAndRejectsWrongLength() throws Exception {
    ActivityController<TestActivity> controller = open();
    java.util.ArrayList<android.view.autofill.AutofillId> fields = new java.util.ArrayList<>();
    for (int i = 0; i < 6; i++) fields.add(org.robolectric.util.ReflectionHelpers.callConstructor(
        android.view.autofill.AutofillId.class,
        org.robolectric.util.ReflectionHelpers.ClassParameter.from(int.class, 70 + i)));
    // Recreate using the same authentication extras as the service's multi-field dataset.
    controller.pause().stop().destroy();
    Intent intent = new Intent(RuntimeEnvironment.getApplication(), OtpAutofillActivity.class)
        .putExtra(OtpAutofillActivity.EXTRA_FIELD, fields.get(0))
        .putParcelableArrayListExtra(OtpAutofillActivity.EXTRA_FIELDS, fields)
        .putExtra(OtpAutofillActivity.EXTRA_PACKAGE, "example.browser");
    controller = Robolectric.buildActivity(TestActivity.class, intent).setup();
    Shadows.shadowOf(Looper.getMainLooper()).idle(); receive(controller.get(), "012345");
    Dataset dataset = Shadows.shadowOf(controller.get()).getResultIntent().getParcelableExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT);
    java.util.List<?> values = (java.util.List<?>)Dataset.class.getDeclaredMethod("getFieldValues").invoke(dataset);
    java.util.List<?> ids = (java.util.List<?>)Dataset.class.getDeclaredMethod("getFieldIds").invoke(dataset);
    assertEquals(fields, ids); assertEquals(6, values.size());
    for (int i = 0; i < 6; i++) assertEquals("012345".substring(i, i + 1),
        ((android.view.autofill.AutofillValue)values.get(i)).getTextValue().toString());
    controller.pause().stop().destroy();
    controller = Robolectric.buildActivity(TestActivity.class, intent).setup();
    Shadows.shadowOf(Looper.getMainLooper()).idle(); receive(controller.get(), "1234");
    assertEquals(Activity.RESULT_CANCELED, Shadows.shadowOf(controller.get()).getResultCode());
    controller.pause().stop().destroy();
  }
}
