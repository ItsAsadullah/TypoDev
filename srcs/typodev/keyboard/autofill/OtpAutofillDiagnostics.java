package typodev.keyboard.autofill;

import android.content.Context;

/** Records only a generic result, never a field, app name, SMS, or verification code. */
final class OtpAutofillDiagnostics {
  static final String PREFS = "otp_autofill_diagnostics";
  static void record(Context context, int message) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt("status", message).apply();
  }
}
