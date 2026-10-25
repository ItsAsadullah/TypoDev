package typodev.keyboard.autofill;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.autofill.AutofillManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import typodev.keyboard.R;

/** Native test field distinguishes provider/retrieval problems from browser form compatibility. */
@android.annotation.TargetApi(28)
public final class OtpAutofillTestActivity extends Activity {
  private TextView status;
  private SharedPreferences diagnostics;
  private final SharedPreferences.OnSharedPreferenceChangeListener changed = (prefs, key) -> runOnUiThread(this::refresh);
  @Override protected void onCreate(Bundle saved) {
    super.onCreate(saved);
    if (Build.VERSION.SDK_INT < 28) { finish(); return; }
    diagnostics = getSharedPreferences(OtpAutofillDiagnostics.PREFS, Context.MODE_PRIVATE);
    OtpAutofillDiagnostics.record(this, R.string.otp_diag_no_request);
    LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL);
    int padding = (int)(24 * getResources().getDisplayMetrics().density);
    layout.setPadding(padding, padding, padding, padding);
    TextView instructions = new TextView(this); instructions.setText(R.string.otp_autofill_test_instructions); layout.addView(instructions);
    EditText field = new EditText(this); field.setId(R.id.otp_autofill_test_field);
    field.setHint(R.string.otp_autofill_test_hint); field.setInputType(InputType.TYPE_CLASS_NUMBER);
    field.setAutofillHints("smsOTPCode"); field.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_YES);
    layout.addView(field);
    Button request = new Button(this); request.setText(R.string.otp_autofill_test_request);
    request.setOnClickListener(view -> {
      field.requestFocus();
      AutofillManager manager = getSystemService(AutofillManager.class);
      if (manager != null && OtpAutofillSettings.isSelected(this)) manager.requestAutofill(field);
      else OtpAutofillSettings.open(this);
      refresh();
    }); layout.addView(request);
    status = new TextView(this); layout.addView(status);
    setContentView(layout); refresh();
  }
  private void refresh() {
    if (status == null) return;
    int message = diagnostics.getInt("status", R.string.otp_diag_no_request);
    status.setText(getString(OtpAutofillSettings.isSelected(this) ? R.string.otp_autofill_selected : R.string.otp_autofill_not_selected)
        + "\n\n" + getString(message));
  }
  @Override protected void onResume() {
    super.onResume();
    if (diagnostics != null) { diagnostics.registerOnSharedPreferenceChangeListener(changed); refresh(); }
  }
  @Override protected void onPause() {
    if (diagnostics != null) diagnostics.unregisterOnSharedPreferenceChangeListener(changed);
    super.onPause();
  }
}
