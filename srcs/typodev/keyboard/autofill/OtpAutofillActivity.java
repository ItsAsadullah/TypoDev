package typodev.keyboard.autofill;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.service.autofill.Dataset;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillManager;
import android.view.autofill.AutofillValue;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.widget.RemoteViews;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import com.google.android.gms.auth.api.phone.SmsCodeAutofillClient;
import com.google.android.gms.auth.api.phone.SmsCodeRetriever;
import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.common.api.Status;
import typodev.keyboard.R;

/** Google consent and SMS retrieval are scoped to one Autofill authentication session. */
@android.annotation.TargetApi(28)
public class OtpAutofillActivity extends Activity {
  static final String EXTRA_FIELD = "typodev.otp.field";
  static final String EXTRA_FIELDS = "typodev.otp.fields";
  static final String EXTRA_PACKAGE = "typodev.otp.package";
  private static final int CONSENT = 6109;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final Runnable timeout = () -> fail();
  private AutofillId field;
  private java.util.ArrayList<AutofillId> fields;
  private String targetPackage;
  private boolean registered, resolving, completed;
  private long deadline;
  private final BroadcastReceiver receiver = new BroadcastReceiver() {
    @Override public void onReceive(Context context, Intent intent) {
      if (completed || isFinishing() || !SmsCodeRetriever.SMS_CODE_RETRIEVED_ACTION.equals(intent.getAction())) return;
      Status status = intent.getParcelableExtra(SmsCodeRetriever.EXTRA_STATUS);
      if (status == null || status.getStatusCode() != CommonStatusCodes.SUCCESS) { fail(); return; }
      String code = intent.getStringExtra(SmsCodeRetriever.EXTRA_SMS_CODE);
      if (!validCode(code) || !OtpAutofillSettings.isSelected(context)) { fail(); return; }
      RemoteViews presentation = new RemoteViews(getPackageName(), android.R.layout.simple_list_item_1);
      presentation.setTextViewText(android.R.id.text1, getString(R.string.otp_autofill_title));
      if (fields.size() > 1 && fields.size() != code.length()) { fail(); return; }
      Dataset.Builder datasetBuilder = new Dataset.Builder(presentation);
      for (int i = 0; i < fields.size(); i++) datasetBuilder.setValue(fields.get(i),
          AutofillValue.forText(fields.size() == 1 ? code : code.substring(i, i + 1)));
      Dataset dataset = datasetBuilder.build();
      completed = true;
      OtpAutofillActivity.this.setResult(RESULT_OK, new Intent().putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, dataset));
      finish();
    }
  };
  static boolean validCode(String code) { return code != null && code.matches("[A-Za-z0-9]{4,10}"); }
  @Override protected void onCreate(Bundle saved) {
    super.onCreate(saved);
    setResult(RESULT_CANCELED);
    field = getIntent().getParcelableExtra(EXTRA_FIELD);
    fields = getIntent().getParcelableArrayListExtra(EXTRA_FIELDS);
    if (fields == null) { fields = new java.util.ArrayList<>(); if (field != null) fields.add(field); }
    targetPackage = getIntent().getStringExtra(EXTRA_PACKAGE);
    if (Build.VERSION.SDK_INT < 28 || field == null || fields.isEmpty() || fields.size() > 8
        || fields.contains(null) || targetPackage == null
        || !OtpAutofillSettings.isSelected(this)) { finish(); return; }
    deadline = saved == null ? SystemClock.elapsedRealtime() + 5 * 60 * 1000L
        : saved.getLong("deadline");
    resolving = saved != null && saved.getBoolean("resolving");
    LinearLayout layout = new LinearLayout(this);
    layout.setOrientation(LinearLayout.VERTICAL);
    int padding = (int)(24 * getResources().getDisplayMetrics().density);
    layout.setPadding(padding, padding, padding, padding);
    TextView message = new TextView(this); message.setText(R.string.otp_autofill_waiting);
    layout.addView(message);
    Button cancel = new Button(this); cancel.setText(android.R.string.cancel);
    cancel.setOnClickListener(view -> finish()); layout.addView(cancel); setContentView(layout);
    ContextCompat.registerReceiver(this, receiver,
        new IntentFilter(SmsCodeRetriever.SMS_CODE_RETRIEVED_ACTION), SmsRetriever.SEND_PERMISSION,
        handler, ContextCompat.RECEIVER_EXPORTED);
    registered = true;
    handler.postDelayed(timeout, Math.max(0, deadline - SystemClock.elapsedRealtime()));
    if (!resolving) checkAndStart();
  }
  private void checkAndStart() {
    SmsCodeAutofillClient client = createClient();
    client.hasOngoingSmsRequest(targetPackage).addOnCompleteListener(this, request -> {
      if (completed || isFinishing()) return;
      if (!request.isSuccessful() || Boolean.TRUE.equals(request.getResult())) { fail(); return; }
      client.checkPermissionState().addOnCompleteListener(this, permission -> {
        if (completed || isFinishing()) return;
        if (!permission.isSuccessful() || permission.getResult() == SmsCodeAutofillClient.PermissionState.DENIED) {
          fail(); return;
        }
        startRetriever();
      });
    });
  }
  private void startRetriever() {
    createClient().startSmsCodeRetriever().addOnFailureListener(this, error -> {
      if (completed || isFinishing()) return;
      if (error instanceof ResolvableApiException && !resolving) {
        try {
          resolving = true;
          ((ResolvableApiException)error).startResolutionForResult(this, CONSENT);
        } catch (android.content.IntentSender.SendIntentException unavailable) { fail(); }
      } else fail();
    });
  }
  protected SmsCodeAutofillClient createClient() { return SmsCodeRetriever.getAutofillClient(this); }
  @Override protected void onActivityResult(int request, int result, Intent data) {
    super.onActivityResult(request, result, data);
    if (request != CONSENT) return;
    resolving = false;
    if (result == RESULT_OK && OtpAutofillSettings.isSelected(this)) checkAndStart();
    else fail();
  }
  @Override protected void onSaveInstanceState(Bundle state) {
    state.putBoolean("resolving", resolving); state.putLong("deadline", deadline);
    super.onSaveInstanceState(state);
  }
  private void fail() {
    if (completed || isFinishing()) return;
    completed = true;
    Toast.makeText(this, R.string.otp_autofill_failed, Toast.LENGTH_LONG).show(); finish();
  }
  @Override protected void onDestroy() {
    completed = true;
    handler.removeCallbacks(timeout);
    if (registered) unregisterReceiver(receiver);
    super.onDestroy();
  }
}
