package typodev.keyboard.ai;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.WindowManager;
import androidx.core.app.NotificationCompat;
import java.nio.ByteBuffer;
import typodev.keyboard.R;

/** One consent token and virtual display per session; frames are used only on request. */
public class MediaProjectionService extends Service {
  public static final String ACTION_CAPTURE = "typodev.keyboard.ai.ACTION_CAPTURE";
  public static final String ACTION_STOP = "typodev.keyboard.ai.STOP_PROJECTION_SERVICE";
  private static final String CHANNEL_ID = "ai_media_projection";
  private static final int NOTIFICATION_ID = 4099;
  private static final String EXTRA_RESULT_CODE = "result_code";
  private static final String EXTRA_RESULT_DATA = "result_data";

  public interface CaptureCallback {
    void onCaptured(byte[] jpegBytes);
    void onError(String errorMessage);
  }

  private static volatile CaptureCallback sCallback;
  private static volatile MediaProjectionService activeSession;
  private MediaProjection projection;
  private VirtualDisplay display;
  private ImageReader reader;
  private Image latestFrame;
  private Handler main;
  private boolean finished;
  private boolean awaitingFrame;
  private android.widget.Toast countdownToast;
  private final Runnable idleStop = () -> deliver(null, "Screen capture session ended.");
  private final Runnable captureTimeout = () -> deliver(null, "Screen capture timed out. Please try again.");

  @Override public IBinder onBind(Intent intent) { return null; }

  @Override public int onStartCommand(Intent intent, int flags, int startId) {
    if (intent == null || !ACTION_CAPTURE.equals(intent.getAction())) {
      deliver(null, "Screen capture cancelled.");
      return START_NOT_STICKY;
    }
    if (projection != null && !finished) {
      prepareCapture();
      return START_NOT_STICKY;
    }
    main = new Handler(Looper.getMainLooper());
    finished = false;
    try {
      createNotificationChannel();
      Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
          .setSmallIcon(R.drawable.ic_ai_screen_scan)
          .setContentTitle(getString(R.string.ai_capture_session_title))
          .setContentText(getString(R.string.ai_capture_session_message))
          .addAction(R.drawable.ic_ai_close, getString(R.string.ai_capture_stop),
              PendingIntent.getService(this, 0,
                  new Intent(this, MediaProjectionService.class).setAction(ACTION_STOP),
                  PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
          .setPriority(NotificationCompat.PRIORITY_LOW).setOngoing(true).build();
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
      else startForeground(NOTIFICATION_ID, notification);
      // Give the consent dialog and translucent activity time to leave the screen.
      main.postDelayed(() -> capture(intent), 450);
    } catch (RuntimeException error) {
      deliver(null, "Unable to start screen capture. Please try again.");
    }
    return START_NOT_STICKY;
  }

  private void capture(Intent intent) {
    if (finished) return;
    Intent consent = intent.getParcelableExtra(EXTRA_RESULT_DATA);
    if (consent == null) { deliver(null, "Screen capture permission is missing. Please try again."); return; }
    try {
      MediaProjectionManager manager = (MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
      if (manager == null) { deliver(null, "Screen capture is unavailable on this device."); return; }
      projection = manager.getMediaProjection(intent.getIntExtra(EXTRA_RESULT_CODE, 0), consent);
      if (projection == null) { deliver(null, "Unable to start screen capture."); return; }
      projection.registerCallback(new MediaProjection.Callback() {
        @Override public void onCapturedContentResize(int width, int height) {
          resizeCapture(width, height);
        }
        @Override public void onStop() {
          if (!finished) deliver(null, "Screen capture stopped. Please try again.");
        }
      }, main);

      WindowManager windows = (WindowManager)getSystemService(WINDOW_SERVICE);
      int width, height;
      if (Build.VERSION.SDK_INT >= 30) {
        Rect bounds = windows.getMaximumWindowMetrics().getBounds();
        width = bounds.width(); height = bounds.height();
      } else {
        android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
        windows.getDefaultDisplay().getRealMetrics(metrics);
        width = metrics.widthPixels; height = metrics.heightPixels;
      }
      reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 3);
      // Install before creating the display so the first frame is never missed.
      reader.setOnImageAvailableListener(this::readFrame, main);
      display = projection.createVirtualDisplay("WritingScreenCapture", width, height,
          getResources().getConfiguration().densityDpi,
          DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader.getSurface(), null, main);
      activeSession = this;
      prepareCapture();
    } catch (RuntimeException error) {
      deliver(null, "Unable to capture this screen. Try Recent screenshot or Choose an image.");
    }
  }

  private void prepareCapture() {
    awaitingFrame = false;
    main.removeCallbacks(idleStop);
    countdownToast = android.widget.Toast.makeText(this, R.string.ai_capture_countdown,
        android.widget.Toast.LENGTH_LONG);
    countdownToast.show();
    main.postDelayed(() -> {
      if (finished || sCallback == null) return;
      countdownToast.cancel();
      main.postDelayed(this::acceptFrame, 400);
    }, 3000);
  }

  private void resizeCapture(int width, int height) {
    if (finished || display == null || reader == null || width <= 0 || height <= 0) return;
    if (reader.getWidth() == width && reader.getHeight() == height) return;
    try {
      ImageReader replacement = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 3);
      replacement.setOnImageAvailableListener(this::readFrame, main);
      try {
        display.resize(width, height, getResources().getConfiguration().densityDpi);
        display.setSurface(replacement.getSurface());
      } catch (RuntimeException error) {
        replacement.close(); throw error;
      }
      if (latestFrame != null) { latestFrame.close(); latestFrame = null; }
      reader.setOnImageAvailableListener(null, null);
      reader.close(); reader = replacement;
    } catch (RuntimeException error) {
      deliver(null, "Screen size changed. Please start capture again.");
    }
  }

  private void acceptFrame() {
    if (finished || sCallback == null) return;
    try {
      awaitingFrame = true;
      main.postDelayed(captureTimeout, 12000);
      if (latestFrame != null) {
        Image frame = latestFrame;
        latestFrame = null;
        processFrame(frame);
      }
    } catch (RuntimeException error) {
      deliver(null, "Unable to capture this screen. Please try again.");
    }
  }

  private void readFrame(ImageReader source) {
    if (finished) return;
    try {
      Image frame = source.acquireLatestImage();
      if (frame == null) return;
      if (sCallback == null) { frame.close(); return; }
      if (!awaitingFrame) {
        if (latestFrame != null) latestFrame.close();
        latestFrame = frame;
        return;
      }
      processFrame(frame);
    } catch (RuntimeException error) {
      deliver(null, "Unable to read this screen. Please try again.");
    }
  }

  private void processFrame(Image image) {
    Bitmap padded = null, cropped = null;
    byte[] bytes = null;
    String failure = null;
    try {
      Image.Plane plane = image.getPlanes()[0];
      ByteBuffer pixels = plane.getBuffer();
      int width = image.getWidth(), height = image.getHeight();
      int strideWidth = plane.getRowStride() / plane.getPixelStride();
      padded = Bitmap.createBitmap(strideWidth, height, Bitmap.Config.ARGB_8888);
      padded.copyPixelsFromBuffer(pixels);
      cropped = strideWidth == width ? padded : Bitmap.createBitmap(padded, 0, 0, width, height);
      bytes = AiVisionHelper.scaleAndCompressBitmap(cropped, 1024, 82);
      if (bytes == null || bytes.length == 0) failure = "Unable to read the screen image. Please try again.";
    } catch (RuntimeException | OutOfMemoryError error) {
      failure = "Unable to read this screen. Try a screenshot instead.";
    } finally {
      // Release the acquired image before closing its ImageReader.
      if (image != null) try { image.close(); } catch (RuntimeException ignored) {}
      if (cropped != null && cropped != padded) cropped.recycle();
      if (padded != null) padded.recycle();
    }
    if (failure != null || bytes == null || bytes.length == 0) {
      deliver(null, failure == null ? "No screen image was captured. Please try again." : failure);
      return;
    }
    awaitingFrame = false;
    main.removeCallbacks(captureTimeout);
    CaptureCallback callback = sCallback;
    sCallback = null;
    main.postDelayed(idleStop, 5 * 60 * 1000L);
    if (callback != null) callback.onCaptured(bytes);
  }

  private void deliver(byte[] bytes, String error) {
    if (finished) return;
    finished = true;
    activeSession = null;
    awaitingFrame = false;
    CaptureCallback callback = sCallback;
    sCallback = null;
    cleanup();
    stopForeground(true);
    stopSelf();
    if (callback != null) new Handler(Looper.getMainLooper()).post(() -> {
      if (error == null && bytes != null && bytes.length > 0) callback.onCaptured(bytes);
      else callback.onError(error == null ? "No screen image was captured. Please try again." : error);
    });
  }

  private void cleanup() {
    if (main != null) main.removeCallbacksAndMessages(null);
    if (countdownToast != null) countdownToast.cancel();
    if (latestFrame != null) {
      try { latestFrame.close(); } catch (RuntimeException ignored) {}
      latestFrame = null;
    }
    if (display != null) {
      try { display.release(); } catch (RuntimeException ignored) {}
      display = null;
    }
    if (reader != null) {
      try { reader.close(); } catch (RuntimeException ignored) {}
      reader = null;
    }
    if (projection != null) {
      try { projection.stop(); } catch (RuntimeException ignored) {}
      projection = null;
    }
  }

  @Override public void onDestroy() {
    if (!finished) deliver(null, "Screen capture was interrupted. Please try again.");
    else cleanup();
    super.onDestroy();
  }

  private void createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Screen capture",
          NotificationManager.IMPORTANCE_LOW);
      channel.setDescription("Captures the screen for Writing Tools");
      NotificationManager manager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
      if (manager != null) manager.createNotificationChannel(channel);
    }
  }

  public static void startCapture(Context context, int resultCode, Intent data, CaptureCallback callback) {
    if (context == null) { if (callback != null) callback.onError("Unable to start screen capture."); return; }
    if (sCallback != null) { if (callback != null) callback.onError("A screen capture is already in progress."); return; }
    sCallback = callback;
    Intent intent = new Intent(context, MediaProjectionService.class).setAction(ACTION_CAPTURE)
        .putExtra(EXTRA_RESULT_CODE, resultCode).putExtra(EXTRA_RESULT_DATA, data);
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent);
      else context.startService(intent);
    } catch (RuntimeException error) {
      sCallback = null;
      if (callback != null) callback.onError("Unable to start screen capture. Please try again.");
    }
  }

  public static boolean hasActiveSession() {
    MediaProjectionService session = activeSession;
    return session != null && !session.finished && session.projection != null;
  }

  public static void captureInActiveSession(Context context, CaptureCallback callback) {
    if (!hasActiveSession()) {
      if (callback != null) callback.onError("Screen capture session ended. Please try again.");
      return;
    }
    startCapture(context, 0, null, callback);
  }

  public static void stop(Context context) {
    if (context == null) return;
    try { context.stopService(new Intent(context, MediaProjectionService.class)); }
    catch (RuntimeException ignored) {}
  }
}
