package typodev.keyboard.clipboard;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import typodev.keyboard.R;

/** A regular editor target lets the IME type the shortcut without editing the source app. */
public final class ClipboardShortcutActivity extends Activity {
  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    String text = getIntent().getStringExtra("clip_text");
    if (text == null || text.trim().isEmpty()) { finish(); return; }
    PinnedClipboardStore store = PinnedClipboardStore.instance(this);
    int pad = Math.round(20 * getResources().getDisplayMetrics().density);
    LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(pad, pad, pad, pad);
    TextView title = new TextView(this); title.setText(R.string.clipboard_shortcut_title);
    title.setTextSize(20); title.setTypeface(null, android.graphics.Typeface.BOLD); root.addView(title);
    TextView preview = new TextView(this); preview.setText(text); preview.setTextSize(14);
    preview.setMaxLines(4); preview.setEllipsize(android.text.TextUtils.TruncateAt.END);
    preview.setPadding(0, pad, 0, pad); root.addView(preview);
    EditText shortcut = new EditText(this); shortcut.setId(R.id.clipboard_shortcut_input);
    shortcut.setSingleLine(true); shortcut.setHint(R.string.clipboard_shortcut_hint);
    shortcut.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
    shortcut.setText(store.getShortcut(text));
    root.addView(shortcut, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    TextView help = new TextView(this); help.setText(R.string.clipboard_shortcut_help);
    help.setTextSize(13); help.setPadding(0, pad / 2, 0, pad); root.addView(help);
    LinearLayout actions = new LinearLayout(this); actions.setGravity(Gravity.END);
    Button cancel = new Button(this); cancel.setText(android.R.string.cancel); cancel.setOnClickListener(v -> finish()); actions.addView(cancel);
    Button save = new Button(this); save.setText(R.string.clipboard_shortcut_save);
    save.setOnClickListener(v -> {
      if (!store.setShortcut(text, shortcut.getText().toString())) {
        shortcut.setError(getString(R.string.clipboard_shortcut_invalid)); return;
      }
      Toast.makeText(this, R.string.clipboard_shortcut_saved, Toast.LENGTH_SHORT).show(); finish();
    }); actions.addView(save); root.addView(actions);
    setContentView(root);
    if (state == null) shortcut.requestFocus();
  }
}
