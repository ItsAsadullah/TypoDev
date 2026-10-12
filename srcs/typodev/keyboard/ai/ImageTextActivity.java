package typodev.keyboard.ai;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import typodev.keyboard.R;

/** Full-resolution camera/gallery input, editable transcription and explicit clipboard controls. */
public final class ImageTextActivity extends Activity {
  private static final int CAMERA = 101, GALLERY = 102;
  private static final int IDLE = 0, PICKING = 1, READING = 2, RESULT = 3;
  private static final ExecutorService IMAGES = Executors.newSingleThreadExecutor();
  private final Handler main = new Handler(Looper.getMainLooper());
  private TextView status;
  private EditText text;
  private ProgressBar progress;
  private Button camera, gallery, copy, retry;
  private File photo;
  private Uri source;
  private int phase;
  private int generation;
  private boolean resumed, copied;

  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    typodev.keyboard.SettingsThemeHelper.applyAdaptiveSystemBars(this);
    setTitle(R.string.image_text_title);
    buildUi();
    if (state == null) return;
    phase = state.getInt("phase", IDLE);
    copied = state.getBoolean("copied", false);
    String path = state.getString("photo");
    if (path != null) photo = new File(path);
    String uri = state.getString("source");
    if (uri != null) source = Uri.parse(uri);
    text.setText(state.getString("text", ""));
    if (phase == READING && source != null) readImage();
    else if (phase == RESULT) showResult(text.getText().toString(), false);
  }

  private void buildUi() {
    LinearLayout root = new LinearLayout(this);
    root.setBackgroundColor(getResources().getColor(R.color.settings_bg));
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp(20), dp(12), dp(20), dp(12));
    root.setOnApplyWindowInsetsListener((view, insets) -> {
      view.setPadding(dp(20) + insets.getSystemWindowInsetLeft(), dp(12) + insets.getSystemWindowInsetTop(),
          dp(20) + insets.getSystemWindowInsetRight(), dp(12) + insets.getSystemWindowInsetBottom());
      return insets.consumeSystemWindowInsets();
    });
    TextView title = new TextView(this);
    title.setText(R.string.image_text_title); title.setTextSize(22);
    title.setTextColor(getResources().getColor(R.color.settings_text_title));
    root.addView(title);
    TextView description = new TextView(this);
    description.setText(R.string.image_text_online); description.setTextSize(13);
    description.setTextColor(getResources().getColor(R.color.settings_text_subtitle));
    description.setPadding(0, dp(8), 0, dp(12)); root.addView(description);
    LinearLayout sources = new LinearLayout(this);
    camera = button(R.string.image_text_camera, v -> chooseCamera());
    gallery = button(R.string.image_text_gallery, v -> chooseGallery());
    sources.addView(camera, new LinearLayout.LayoutParams(0, -2, 1));
    sources.addView(gallery, new LinearLayout.LayoutParams(0, -2, 1)); root.addView(sources);
    progress = new ProgressBar(this);
    progress.setVisibility(View.GONE);
    LinearLayout.LayoutParams loading = new LinearLayout.LayoutParams(dp(40), dp(40));
    loading.gravity = Gravity.CENTER_HORIZONTAL; root.addView(progress, loading);
    status = new TextView(this); status.setPadding(0, dp(8), 0, dp(8)); root.addView(status);
    status.setTextColor(getResources().getColor(R.color.settings_text_subtitle));
    text = new EditText(this);
    text.setHint(R.string.image_text_hint); text.setGravity(Gravity.TOP | Gravity.START);
    text.setTextColor(getResources().getColor(R.color.settings_text_title));
    text.setHintTextColor(getResources().getColor(R.color.settings_text_subtitle));
    text.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
    text.setImeOptions(EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING);
    text.setTextSize(16); text.setEnabled(false);
    root.addView(text, new LinearLayout.LayoutParams(-1, 0, 1));
    copy = button(R.string.image_text_copy, v -> copyText()); copy.setEnabled(false); root.addView(copy);
    retry = button(R.string.image_text_retry, v -> { if (source != null) readImage(); });
    retry.setVisibility(View.GONE); root.addView(retry);
    root.addView(button(android.R.string.cancel, v -> finish()));
    setContentView(root);
  }

  private Button button(int label, View.OnClickListener action) {
    Button button = new Button(this); button.setText(label); button.setAllCaps(false);
    button.setTextColor(getResources().getColor(R.color.settings_text_title));
    button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.settings_surface)));
    button.setMinHeight(dp(48)); button.setOnClickListener(action); return button;
  }
  private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

  private boolean ready() {
    if (phase == READING || phase == PICKING) return false;
    if (GeminiAiService.getApiKey(this).isEmpty()) {
      Toast.makeText(this, R.string.image_text_setup, Toast.LENGTH_LONG).show(); return false;
    }
    return true;
  }
  private void chooseGallery() {
    if (!ready()) return;
    try {
      phase = PICKING;
      startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT).setType("image/*")
          .addCategory(Intent.CATEGORY_OPENABLE), GALLERY);
    } catch (RuntimeException error) { phase = IDLE; showError(getString(R.string.image_text_failed)); }
  }
  private void chooseCamera() {
    if (!ready()) return;
    try {
      deletePhoto();
      File directory = new File(getCacheDir(), "ocr_camera");
      if (!directory.isDirectory() && !directory.mkdirs()) throw new IllegalStateException();
      photo = File.createTempFile("photo-", ".jpg", directory);
      Uri output = FileProvider.getUriForFile(this, getPackageName() + ".clipboard.images", photo);
      Intent capture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT, output)
          .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
      capture.setClipData(ClipData.newRawUri("photo", output));
      phase = PICKING; startActivityForResult(capture, CAMERA);
    } catch (Exception error) {
      phase = IDLE; deletePhoto(); showError(getString(R.string.image_text_camera_missing));
    }
  }
  @Override public void onActivityResult(int request, int result, Intent data) {
    super.onActivityResult(request, result, data);
    if (request != CAMERA && request != GALLERY) return;
    phase = IDLE;
    if (result != RESULT_OK) { if (request == CAMERA) deletePhoto(); return; }
    if (request == CAMERA) {
      if (photo == null || !photo.isFile() || photo.length() == 0) {
        showError(getString(R.string.image_text_failed)); return;
      }
      source = FileProvider.getUriForFile(this, getPackageName() + ".clipboard.images", photo);
    } else source = data == null ? null : data.getData();
    if (source == null) { showError(getString(R.string.image_text_failed)); return; }
    readImage();
  }

  private void readImage() {
    final int request = ++generation;
    final Uri selected = source;
    phase = READING; copied = false;
    text.setText(""); text.setEnabled(false); copy.setEnabled(false);
    camera.setEnabled(false); gallery.setEnabled(false);
    retry.setVisibility(View.GONE); progress.setVisibility(View.VISIBLE);
    status.setText(R.string.image_text_reading);
    IMAGES.execute(() -> {
      byte[] jpeg = AiVisionHelper.getBytesFromUri(getApplicationContext(), selected, 2048);
      main.post(() -> {
        if (!current(request)) return;
        if (jpeg == null || jpeg.length == 0) { showError(getString(R.string.image_text_failed)); return; }
        GeminiAiService.extractImageText(getApplicationContext(), jpeg, new GeminiAiService.AiCallback() {
          @Override public void onSuccess(String response) {
            if (current(request)) showResult(ImageTextExtractor.clean(response), true);
          }
          @Override public void onError(String error) { if (current(request)) showError(error); }
        });
      });
    });
  }
  private boolean current(int request) { return request == generation && !isFinishing() && !isDestroyed(); }
  private void stopLoading() {
    progress.setVisibility(View.GONE); camera.setEnabled(true); gallery.setEnabled(true);
  }
  private void showResult(String result, boolean autoCopy) {
    phase = RESULT; stopLoading(); text.setText(result); text.setEnabled(true);
    copy.setEnabled(!result.trim().isEmpty()); retry.setVisibility(View.GONE);
    status.setText(result.trim().isEmpty() ? R.string.image_text_empty
        : copied ? R.string.image_text_copied : R.string.image_text_copy);
    if (autoCopy && resumed && !result.trim().isEmpty()) copyText();
  }
  private void showError(String error) {
    phase = IDLE; stopLoading();
    status.setText(error == null ? getString(R.string.image_text_failed) : error);
    retry.setVisibility(source == null ? View.GONE : View.VISIBLE);
  }
  private void copyText() {
    String extracted = text.getText().toString();
    if (extracted.trim().isEmpty()) return;
    try {
      ClipboardManager clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
      if (clipboard == null) throw new IllegalStateException();
      clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.image_text_title), extracted));
      copied = true; status.setText(R.string.image_text_copied);
    } catch (RuntimeException error) { status.setText(R.string.image_text_copy_failed); }
  }
  @Override public void onResume() {
    super.onResume(); resumed = true;
    if (phase == RESULT && !copied && text.length() > 0) copyText();
  }
  @Override public void onPause() { resumed = false; super.onPause(); }
  @Override public void onSaveInstanceState(Bundle state) {
    state.putInt("phase", phase); state.putBoolean("copied", copied);
    state.putString("text", text.getText().toString());
    if (source != null) state.putString("source", source.toString());
    if (photo != null) state.putString("photo", photo.getAbsolutePath());
    super.onSaveInstanceState(state);
  }
  private void deletePhoto() {
    if (photo != null) {
      try { revokeUriPermission(FileProvider.getUriForFile(this, getPackageName() + ".clipboard.images", photo),
          Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION); } catch (RuntimeException ignored) { }
      photo.delete(); photo = null;
    }
  }
  @Override public void onDestroy() {
    generation++; main.removeCallbacksAndMessages(null);
    if (isFinishing()) deletePhoto();
    super.onDestroy();
  }
}
