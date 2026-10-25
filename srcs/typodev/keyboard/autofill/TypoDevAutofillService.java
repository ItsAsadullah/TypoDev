package typodev.keyboard.autofill;

import android.app.PendingIntent;
import android.app.assist.AssistStructure;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.service.autofill.AutofillService;
import android.service.autofill.Dataset;
import android.service.autofill.FillCallback;
import android.service.autofill.FillRequest;
import android.service.autofill.FillResponse;
import android.service.autofill.SaveCallback;
import android.service.autofill.SaveRequest;
import android.widget.RemoteViews;
import com.google.android.gms.auth.api.phone.SmsCodeAutofillClient;
import com.google.android.gms.auth.api.phone.SmsCodeRetriever;
import java.util.concurrent.atomic.AtomicBoolean;
import typodev.keyboard.R;

/** Supplies a user-selected authentication dataset, never holds a FillCallback while waiting for SMS. */
@android.annotation.TargetApi(28)
public final class TypoDevAutofillService extends AutofillService {
  @Override public void onFillRequest(FillRequest request, CancellationSignal cancellation, FillCallback callback) {
    OtpAutofillDiagnostics.record(this, R.string.otp_diag_request);
    if (Build.VERSION.SDK_INT < 28 || !OtpAutofillSettings.isSelected(this)
        || request.getFillContexts().isEmpty()) { callback.onSuccess(null); return; }
    AssistStructure structure = request.getFillContexts().get(request.getFillContexts().size() - 1).getStructure();
    java.util.List<AssistStructure.ViewNode> fields = OtpFieldDetector.findFields(structure);
    if (fields.isEmpty() || structure.getActivityComponent() == null) {
      OtpAutofillDiagnostics.record(this, R.string.otp_diag_no_field);
      callback.onSuccess(null); return;
    }
    String targetPackage = structure.getActivityComponent().getPackageName();
    if (getPackageName().equals(targetPackage) && !OtpAutofillTestActivity.class.getName()
        .equals(structure.getActivityComponent().getClassName())) { callback.onSuccess(null); return; }
    Handler handler = new Handler(Looper.getMainLooper());
    AtomicBoolean finished = new AtomicBoolean();
    Runnable timeout = () -> {
      if (finished.compareAndSet(false, true)) {
        OtpAutofillDiagnostics.record(this, R.string.otp_diag_unavailable);
        callback.onSuccess(null);
      }
    };
    handler.postDelayed(timeout, 8000);
    cancellation.setOnCancelListener(() -> { finished.set(true); handler.removeCallbacks(timeout); });
    SmsCodeAutofillClient client = SmsCodeRetriever.getAutofillClient(this);
    client.hasOngoingSmsRequest(targetPackage).addOnCompleteListener(ongoing -> {
      if (finished.get()) return;
      if (!ongoing.isSuccessful() || Boolean.TRUE.equals(ongoing.getResult())) {
        if (finished.compareAndSet(false, true)) {
          handler.removeCallbacks(timeout);
          OtpAutofillDiagnostics.record(this, ongoing.isSuccessful() ? R.string.otp_diag_ongoing : R.string.otp_diag_unavailable);
          callback.onSuccess(null);
        }
        return;
      }
      client.checkPermissionState().addOnCompleteListener(permission -> {
        if (!finished.compareAndSet(false, true)) return;
        handler.removeCallbacks(timeout);
        if (!permission.isSuccessful() || permission.getResult() == SmsCodeAutofillClient.PermissionState.DENIED) {
          OtpAutofillDiagnostics.record(this, permission.isSuccessful() ? R.string.otp_diag_denied : R.string.otp_diag_unavailable);
          callback.onSuccess(null); return;
        }
        java.util.ArrayList<android.view.autofill.AutofillId> ids = new java.util.ArrayList<>();
        for (AssistStructure.ViewNode field : fields) ids.add(field.getAutofillId());
        Intent intent = new Intent(this, OtpAutofillActivity.class)
            .setData(Uri.parse("typodev-otp:" + java.util.UUID.randomUUID()))
            .putExtra(OtpAutofillActivity.EXTRA_FIELD, ids.get(0))
            .putParcelableArrayListExtra(OtpAutofillActivity.EXTRA_FIELDS, ids)
            .putExtra(OtpAutofillActivity.EXTRA_PACKAGE, targetPackage);
        // Android adds the authentication extras when sending this PendingIntent.
        int flags = PendingIntent.FLAG_CANCEL_CURRENT;
        if (Build.VERSION.SDK_INT >= 31) flags |= PendingIntent.FLAG_MUTABLE;
        PendingIntent authentication = PendingIntent.getActivity(this, 0, intent, flags);
        Dataset.Builder datasetBuilder = new Dataset.Builder(presentation(getString(R.string.otp_autofill_prompt)));
        for (android.view.autofill.AutofillId id : ids) datasetBuilder.setValue(id, null);
        Dataset dataset = datasetBuilder.setAuthentication(authentication.getIntentSender()).build();
        OtpAutofillDiagnostics.record(this, R.string.otp_diag_prompt);
        callback.onSuccess(new FillResponse.Builder().addDataset(dataset).build());
      });
    });
  }
  private RemoteViews presentation(String text) {
    RemoteViews view = new RemoteViews(getPackageName(), android.R.layout.simple_list_item_1);
    view.setTextViewText(android.R.id.text1, text);
    return view;
  }
  @Override public void onSaveRequest(SaveRequest request, SaveCallback callback) {
    // One-time codes are never saved as credentials.
    callback.onSuccess();
  }
}
