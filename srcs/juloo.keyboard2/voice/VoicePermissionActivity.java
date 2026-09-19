package juloo.keyboard2.voice;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Window;
import android.widget.Toast;
import juloo.keyboard2.Keyboard2;

/**
 * Transparent helper activity to request runtime microphone permission
 * on behalf of Keyboard2 InputMethodService.
 */
public class VoicePermissionActivity extends Activity
{
  private static final int PERMISSION_REQ_CODE = 4021;
  private static Keyboard2 sPendingKeyboard = null;

  public static void requestVoicePermission(Context context, Keyboard2 keyboard)
  {
    sPendingKeyboard = keyboard;
    Intent intent = new Intent(context, VoicePermissionActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    context.startActivity(intent);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    overridePendingTransition(0, 0);
    requestWindowFeature(Window.FEATURE_NO_TITLE);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
    {
      if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
      {
        startPendingListening();
        finish();
        return;
      }
      requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQ_CODE);
    }
    else
    {
      startPendingListening();
      finish();
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
  {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == PERMISSION_REQ_CODE)
    {
      boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
      if (granted)
      {
        startPendingListening();
      }
      else
      {
        Toast.makeText(this, "ভয়েস টাইপিংয়ের জন্য মাইক্রোফোন পারমিশন প্রয়োজন", Toast.LENGTH_SHORT).show();
        sPendingKeyboard = null;
      }
    }
    finish();
  }

  @Override
  public void finish()
  {
    super.finish();
    overridePendingTransition(0, 0);
  }

  private void startPendingListening()
  {
    final Keyboard2 kb = sPendingKeyboard;
    sPendingKeyboard = null;
    if (kb != null)
    {
      new Handler(Looper.getMainLooper()).post(new Runnable()
      {
        @Override
        public void run()
        {
          OfflineVoiceTypingService.startListening(kb, kb);
        }
      });
    }
  }
}
