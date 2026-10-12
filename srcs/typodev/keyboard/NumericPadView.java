package typodev.keyboard;

import android.graphics.drawable.GradientDrawable;
import android.graphics.Insets;
import android.os.Build;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Numeric editors always open in ASCII mode, independent of the text layout. */
public final class NumericPadView extends LinearLayout {
  private final Keyboard2 keyboard;
  private final EditorInfo editor;
  private boolean bengali, calculator;
  private String expression = "";
  private TextView display;
  private final int background, keyColor, label, accent;
  public NumericPadView(Keyboard2 keyboard, EditorInfo editor) {
    super(keyboard);
    this.keyboard = keyboard; this.editor = editor;
    background = keyboard.getColorKeyboard(); keyColor = keyboard.getColorKey();
    label = keyboard.getColorLabel(); accent = keyboard.getColorKeyActivated();
    setOrientation(VERTICAL); setBackgroundColor(background); setPadding(dp(3), dp(3), dp(3), dp(3));
    build();
  }
  private int dp(float n) { return Math.round(n * getResources().getDisplayMetrics().density); }
  @Override protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    requestApplyInsets();
    if (Build.VERSION.SDK_INT >= 35 && getRootWindowInsets() != null)
      applySystemInsets(getRootWindowInsets());
  }
  @Override public WindowInsets onApplyWindowInsets(WindowInsets insets) {
    // The IME window draws behind system bars starting on Android 15.
    if (Build.VERSION.SDK_INT < 35) return super.onApplyWindowInsets(insets);
    applySystemInsets(insets);
    return WindowInsets.CONSUMED;
  }
  private void applySystemInsets(WindowInsets windowInsets) {
    if (Build.VERSION.SDK_INT < 35) return;
    Insets insets = windowInsets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
    setPadding(dp(3) + insets.left, dp(3), dp(3) + insets.right, dp(3) + insets.bottom);
  }
  private boolean numberEditor() { return (editor.inputType & InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_NUMBER; }
  private boolean password() { return numberEditor() && (editor.inputType & InputType.TYPE_MASK_VARIATION) == InputType.TYPE_NUMBER_VARIATION_PASSWORD; }
  private String digits(String text) {
    if (!bengali) return text;
    StringBuilder result = new StringBuilder();
    for (char c : text.toCharArray()) result.append(c >= '0' && c <= '9' ? (char)('০' + c - '0') : c);
    return result.toString();
  }
  private LinearLayout row() {
    LinearLayout row = new LinearLayout(getContext()); row.setOrientation(HORIZONTAL);
    addView(row, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)); return row;
  }
  private void key(LinearLayout row, String text, String description, boolean action, Runnable click) {
    TextView button = new TextView(getContext()); button.setText(text); button.setTextColor(label);
    button.setTextSize(text.length() > 3 ? 15 : 24); button.setGravity(Gravity.CENTER);
    button.setContentDescription(description); button.setFocusable(false);
    GradientDrawable shape = new GradientDrawable(); shape.setColor(action ? accent : keyColor); shape.setCornerRadius(dp(10));
    button.setBackground(shape);
    LayoutParams params = new LayoutParams(0, dp(48), 1); params.setMargins(dp(2), dp(2), dp(2), dp(2));
    row.addView(button, params); button.setOnClickListener(v -> {
      v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP); click.run();
    });
  }
  private void build() {
    removeAllViews();
    LinearLayout controls = row();
    key(controls, bengali ? "English" : "বাংলা", "Switch number language", true, () -> { bengali = !bengali; build(); });
    if (numberEditor() && !password()) key(controls, calculator ? getResources().getString(R.string.numeric_numbers) : getResources().getString(R.string.numeric_calculator), "Toggle calculator", true,
        () -> { calculator = !calculator; expression = ""; build(); });
    if (calculator) {
      display = new TextView(getContext()); display.setTextSize(22); display.setTextColor(label);
      display.setGravity(Gravity.END | Gravity.CENTER_VERTICAL); display.setSingleLine(true);
      display.setPadding(dp(8), 0, dp(8), 0); display.setText(digits(expression));
      addView(display, new LayoutParams(LayoutParams.MATCH_PARENT, dp(40)));
    }
    String[][] rows = calculator ? new String[][]{{"7","8","9","/"},{"4","5","6","*"},{"1","2","3","-"},{".","0","=","+"}}
        : new String[][]{{"1","2","3","⌫"},{"4","5","6",numberEditor() ? "-" : "+"},{"7","8","9",numberEditor() ? "." : "#"},{"*","0",",","↵"}};
    for (String[] values : rows) {
      LinearLayout row = row();
      for (String value : values) {
        boolean digit = value.length() == 1 && Character.isDigit(value.charAt(0));
        key(row, calculator ? digits(value).replace('*', '×').replace('/', '÷') : digits(value), value, !digit, () -> press(value));
        View button = row.getChildAt(row.getChildCount() - 1);
        if (!calculator && numberEditor() && !digit && !value.equals("⌫") && !value.equals("↵")) {
          boolean allowed = value.equals(".") && (editor.inputType & InputType.TYPE_NUMBER_FLAG_DECIMAL) != 0
              || value.equals("-") && (editor.inputType & InputType.TYPE_NUMBER_FLAG_SIGNED) != 0;
          button.setEnabled(allowed); button.setAlpha(allowed ? 1f : .3f);
        }
      }
    }
    if (calculator) {
      LinearLayout actions = row();
      key(actions, "C", getResources().getString(R.string.numeric_clear), true, () -> {
        expression = ""; display.setText(""); clearEditor();
      });
      key(actions, "⌫", getResources().getString(R.string.numeric_delete), true, () -> press("⌫"));
    }
  }
  private void deleteEditor(InputConnection connection) {
    CharSequence selected = connection.getSelectedText(0);
    if (selected != null && selected.length() > 0) connection.commitText("", 1);
    else if (!connection.deleteSurroundingText(1, 0)) {
      connection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
      connection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
    }
  }
  private void clearEditor() {
    InputConnection connection = keyboard.getCurrentInputConnection();
    if (connection == null) return;
    connection.beginBatchEdit();
    try {
      ExtractedText text = connection.getExtractedText(new ExtractedTextRequest(), 0);
      if (text != null && text.text != null && text.partialStartOffset < 0
          && connection.setSelection(text.startOffset, text.startOffset + text.text.length())) {
        connection.commitText("", 1); return;
      }
      if (connection.performContextMenuAction(android.R.id.selectAll)) {
        connection.commitText("", 1); return;
      }
      CharSequence before = connection.getTextBeforeCursor(4096, 0), after = connection.getTextAfterCursor(4096, 0);
      CharSequence selected = connection.getSelectedText(0);
      if (selected != null && selected.length() > 0) connection.commitText("", 1);
      connection.deleteSurroundingText(before == null ? 0 : before.length(), after == null ? 0 : after.length());
    } finally { connection.endBatchEdit(); }
  }
  private void press(String value) {
    InputConnection connection = keyboard.getCurrentInputConnection();
    if (calculator) {
      if (value.equals("⌫")) {
        if (!expression.isEmpty()) expression = expression.substring(0, expression.length() - 1);
        else if (connection != null) deleteEditor(connection);
      }
      else if (value.equals("=")) {
        try {
          String result = NumericCalculator.evaluate(expression);
          if (result.contains(".") && (editor.inputType & InputType.TYPE_NUMBER_FLAG_DECIMAL) == 0
              || result.startsWith("-") && (editor.inputType & InputType.TYPE_NUMBER_FLAG_SIGNED) == 0)
            throw new IllegalArgumentException();
          if (connection != null && connection.commitText(digits(result), 1)) { expression = ""; calculator = false; build(); }
          return;
        } catch (ArithmeticException | IllegalArgumentException error) {
          android.widget.Toast.makeText(getContext(), R.string.numeric_invalid_result, android.widget.Toast.LENGTH_SHORT).show(); return;
        }
      } else if (expression.length() < 128) expression += value;
      display.setText(digits(expression)); return;
    }
    if (connection == null) return;
    if (value.equals("⌫")) {
      deleteEditor(connection);
    } else if (value.equals("↵")) {
      int action = editor.imeOptions & EditorInfo.IME_MASK_ACTION;
      if (editor.actionLabel != null) connection.performEditorAction(editor.actionId);
      else if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) connection.performEditorAction(action);
      else { connection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)); connection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER)); }
    } else connection.commitText(digits(value), 1);
  }
}
