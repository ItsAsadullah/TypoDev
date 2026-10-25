package typodev.keyboard.autofill;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;
import typodev.keyboard.R;

public final class OtpAutofillSettings {
  private OtpAutofillSettings() {}
  public static boolean isSelected(Context context) {
    if (Build.VERSION.SDK_INT < 28) return false;
    String selected = Settings.Secure.getString(context.getContentResolver(), "autofill_service");
    return new ComponentName(context, TypoDevAutofillService.class)
        .equals(selected == null ? null : ComponentName.unflattenFromString(selected));
  }
  public static void open(Context context) {
    if (Build.VERSION.SDK_INT < 28) {
      Toast.makeText(context, R.string.otp_autofill_unsupported, Toast.LENGTH_LONG).show();
      return;
    }
    Intent intent = new Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE,
        Uri.parse("package:" + context.getPackageName()));
    try { context.startActivity(intent); }
    catch (android.content.ActivityNotFoundException unavailable) {
      try { context.startActivity(new Intent(Settings.ACTION_SETTINGS)); }
      catch (android.content.ActivityNotFoundException ignored) { /* No settings app. */ }
    }
  }
  public static void openBrowserSettings(Context context) {
    Intent intent = new Intent(Intent.ACTION_APPLICATION_PREFERENCES)
        .addCategory(Intent.CATEGORY_DEFAULT).addCategory(Intent.CATEGORY_APP_BROWSER)
        .addCategory(Intent.CATEGORY_PREFERENCE);
    try { context.startActivity(Intent.createChooser(intent, context.getString(R.string.otp_browser_title))); }
    catch (android.content.ActivityNotFoundException unavailable) {
      Toast.makeText(context, R.string.otp_browser_summary, Toast.LENGTH_LONG).show();
    }
  }
}
