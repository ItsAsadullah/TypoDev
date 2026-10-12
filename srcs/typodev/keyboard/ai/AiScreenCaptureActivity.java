package typodev.keyboard.ai;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Window;
import android.widget.Toast;

/**
 * Transparent helper activity that coordinates:
 * 1. Screen Scan (MediaProjection)
 * 2. Recent Screenshot analysis
 * 3. Gallery image picking
 * without interrupting the user's workflow or cluttering recents.
 */
public class AiScreenCaptureActivity extends Activity
{
  public static final String ACTION_SCREEN_SCAN = "typodev.keyboard.ai.ACTION_SCREEN_SCAN";
  public static final String ACTION_GALLERY_PICK = "typodev.keyboard.ai.ACTION_GALLERY_PICK";
  public static final String ACTION_RECENT_SCREENSHOT = "typodev.keyboard.ai.ACTION_RECENT_SCREENSHOT";

  private static final int REQUEST_MEDIA_PROJECTION = 5011;
  private static final int REQUEST_GALLERY_PICK = 5012;
  private static final int REQUEST_STORAGE_PERMISSION = 5013;

  public interface AiVisionCallback
  {
    void onImageCaptured(byte[] jpegBytes, String tone);
    void onError(String errorMessage);
  }

  private static AiVisionCallback sCallback;
  private static String sPendingTone = "স্বাভাবিক";

  private MediaProjectionManager mMediaProjectionManager;
  private boolean mCaptureHandedOff;

  public static void startScreenScan(Context context, String tone, AiVisionCallback callback)
  {
    launch(context, ACTION_SCREEN_SCAN, tone, callback);
  }

  public static void pickFromGallery(Context context, String tone, AiVisionCallback callback)
  {
    launch(context, ACTION_GALLERY_PICK, tone, callback);
  }

  public static void processRecentScreenshot(Context context, String tone, AiVisionCallback callback)
  {
    launch(context, ACTION_RECENT_SCREENSHOT, tone, callback);
  }

  private static void launch(Context context, String action, String tone, AiVisionCallback callback)
  {
    if (context == null) {
      if (callback != null) callback.onError("Unable to open this tool. Please try again.");
      return;
    }
    if (sCallback != null) {
      if (callback != null) callback.onError("An image request is already in progress.");
      return;
    }
    sCallback = callback;
    sPendingTone = (tone != null) ? tone : "স্বাভাবিক";
    Intent intent = new Intent(context, AiScreenCaptureActivity.class);
    intent.setAction(action);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
    try { context.startActivity(intent); }
    catch (RuntimeException error) { notifyError("Unable to open this tool. Please try again."); }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    overridePendingTransition(0, 0);
    requestWindowFeature(Window.FEATURE_NO_TITLE);

    // Android restores pending permission/picker results after recreation.
    if (savedInstanceState != null) {
      if (sCallback == null) {
        Toast.makeText(this, "Image request interrupted. Please try again.", Toast.LENGTH_LONG).show();
        finish();
      }
      return;
    }
    String action = getIntent() != null ? getIntent().getAction() : null;

    if (ACTION_SCREEN_SCAN.equals(action))
    {
      handleScreenScanRequest();
    }
    else if (ACTION_GALLERY_PICK.equals(action))
    {
      handleGalleryPickRequest();
    }
    else if (ACTION_RECENT_SCREENSHOT.equals(action))
    {
      handleRecentScreenshotRequest();
    }
    else
    {
      finish();
    }
  }

  private void handleScreenScanRequest()
  {
    if (MediaProjectionService.hasActiveSession()) {
      mCaptureHandedOff = true;
      MediaProjectionService.captureInActiveSession(this, new MediaProjectionService.CaptureCallback() {
        @Override public void onCaptured(byte[] jpegBytes) { deliverSuccess(jpegBytes); }
        @Override public void onError(String errorMessage) { notifyError(errorMessage); }
      });
      finish();
      return;
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
    {
      mMediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
      if (mMediaProjectionManager != null)
      {
        try {
          startActivityForResult(mMediaProjectionManager.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION);
          return;
        } catch (RuntimeException error) {
          notifyError("Unable to request screen capture. Please try again.");
          finish();
          return;
        }
      }
    }
    notifyError("Screen capture is unavailable on this device.");
    finish();
  }

  private void handleGalleryPickRequest()
  {
    try
    {
      Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
      intent.setType("image/*");
      intent.addCategory(Intent.CATEGORY_OPENABLE);
      startActivityForResult(Intent.createChooser(intent, "Choose an image"), REQUEST_GALLERY_PICK);
    }
    catch (Throwable t)
    {
      notifyError("Unable to open the photo picker.");
      finish();
    }
  }

  private void handleRecentScreenshotRequest()
  {
    if (hasStoragePermission())
    {
      processLatestScreenshot();
    }
    else
    {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
      {
        requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, REQUEST_STORAGE_PERMISSION);
      }
      else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
      {
        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
      }
      else
      {
        processLatestScreenshot();
      }
    }
  }

  private boolean hasStoragePermission()
  {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
    {
      return checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
    {
      return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }
    return true;
  }

  private void processLatestScreenshot()
  {
    Uri screenshotUri = AiVisionHelper.getLatestScreenshotUri(this);
    if (screenshotUri != null)
    {
      byte[] bytes = AiVisionHelper.getBytesFromUri(this, screenshotUri, 1024);
      if (bytes != null && bytes.length > 0)
      {
        deliverSuccess(bytes);
        finish();
        return;
      }
    }

    Toast.makeText(this, "No recent screenshot found. Choose an image from your photos.", Toast.LENGTH_SHORT).show();
    // Fallback to gallery
    handleGalleryPickRequest();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
  {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == REQUEST_STORAGE_PERMISSION)
    {
      if (grantResults != null && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
      {
        processLatestScreenshot();
      }
      else
      {
        notifyError("Screenshot access was not granted.");
        Toast.makeText(this, "Allow photo access to use a screenshot.", Toast.LENGTH_SHORT).show();
        finish();
      }
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data)
  {
    super.onActivityResult(requestCode, resultCode, data);

    if (requestCode == REQUEST_MEDIA_PROJECTION)
    {
      if (resultCode == RESULT_OK && data != null)
      {
        // Start while this activity is foreground; the service waits for the
        // permission window and helper activity to disappear before reading a frame.
        mCaptureHandedOff = true;
        MediaProjectionService.startCapture(this, resultCode, data,
            new MediaProjectionService.CaptureCallback()
        {
          @Override public void onCaptured(byte[] jpegBytes) { deliverSuccess(jpegBytes); }
          @Override public void onError(String errorMessage) { notifyError(errorMessage); }
        });
        finish();
      }
      else
      {
        notifyError("Screen capture cancelled.");
        finish();
      }
    }
    else if (requestCode == REQUEST_GALLERY_PICK)
    {
      if (resultCode == RESULT_OK && data != null && data.getData() != null)
      {
        Uri selectedImageUri = data.getData();
        byte[] bytes = AiVisionHelper.getBytesFromUri(this, selectedImageUri, 1024);
        if (bytes != null && bytes.length > 0)
        {
          deliverSuccess(bytes);
        }
        else
        {
          notifyError("Unable to load the image.");
        }
      }
      else notifyError("Image selection cancelled.");
      finish();
    }
  }

  private static void deliverSuccess(final byte[] jpegBytes)
  {
    final AiVisionCallback cb = sCallback;
    final String tone = sPendingTone;
    sCallback = null;

    if (cb != null)
    {
      new Handler(Looper.getMainLooper()).post(new Runnable()
      {
        @Override
        public void run()
        {
          cb.onImageCaptured(jpegBytes, tone);
        }
      });
    }
  }

  private static void notifyError(final String errorMessage)
  {
    final AiVisionCallback cb = sCallback;
    sCallback = null;

    if (cb != null)
    {
      new Handler(Looper.getMainLooper()).post(new Runnable()
      {
        @Override
        public void run()
        {
          cb.onError(errorMessage);
        }
      });
    }
  }

  @Override
  protected void onDestroy()
  {
    if (isFinishing() && !mCaptureHandedOff && sCallback != null)
      notifyError("Image request cancelled.");
    super.onDestroy();
  }

  @Override
  public void finish()
  {
    super.finish();
    overridePendingTransition(0, 0);
  }
}
