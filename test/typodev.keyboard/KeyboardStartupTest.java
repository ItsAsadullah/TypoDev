package typodev.keyboard;

import android.text.InputType;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.shadows.ShadowLog;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk = {34, 35})
public class KeyboardStartupTest {
  @Test public void keyboardCanCreateAndOpenTextEditor() {
    ShadowLog.stream = System.out;
    Keyboard2 keyboard = Robolectric.buildService(Keyboard2.class).create().get();
    EditorInfo info = new EditorInfo();
    info.inputType = InputType.TYPE_CLASS_TEXT;
    info.packageName = "test.editor";
    keyboard.onStartInput(info, false);
    keyboard.onStartInputView(info, false);
    View container = ReflectionHelpers.getField(keyboard, "_keyboard_container_view");
    assertNotNull("Keyboard must finish inflating", container);
    assertNotNull(container.findViewById(R.id.keyboard_view));
    container.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.AT_MOST));
    container.layout(0, 0, container.getMeasuredWidth(), container.getMeasuredHeight());
    assertTrue(container.getMeasuredHeight() > 0);
    container.draw(new android.graphics.Canvas(android.graphics.Bitmap.createBitmap(
        1080, 1920, android.graphics.Bitmap.Config.ARGB_8888)));
    for (ShadowLog.LogItem log : ShadowLog.getLogsForTag("typodev.keyboard")) {
      if (log.type >= android.util.Log.ERROR) throw new AssertionError(log.msg, log.throwable);
    }
    keyboard.onDestroy();
  }

  @Test public void calculatorAndAlgebraWorkInAndroidRuntime() {
    assertEquals("6", NumericCalculator.evaluateInline("2+4").result);
    assertNotNull(AlgebraFormulaEngine.evaluateInline("(a+b)2"));
    assertTrue(AlgebraFormulaEngine.isMathProblem("sqrt(144) ="));
  }

  @Test public void solverCanOpenFromKeyboardService() {
    Keyboard2 keyboard = Robolectric.buildService(Keyboard2.class).create().get();
    try {
      keyboard.showMathSolver("");
      android.view.ViewGroup container = ReflectionHelpers.getField(keyboard, "_keyboard_container_view");
      assertNotNull(container.findViewWithTag("math_solver_pane"));
      assertEquals(View.GONE, container.findViewById(R.id.keyboard_wrapper).getVisibility());
      assertNull(org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog());
      assertTrue(keyboard.onKeyDown(android.view.KeyEvent.KEYCODE_BACK,
          new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_BACK)));
      assertNull(container.findViewWithTag("math_solver_pane"));
      assertEquals(View.VISIBLE, container.findViewById(R.id.keyboard_wrapper).getVisibility());
      keyboard.showMathSolver("(a+b)^2");
      assertNotNull(container.findViewWithTag("math_solver_pane"));
      keyboard.onFinishInput();
      assertNull(container.findViewWithTag("math_solver_pane"));
    } finally {
      keyboard.onDestroy();
    }
  }

  @Test public void banglaLayoutPersistsAfterClipboardDismissAndPreferenceSync() {
    Keyboard2 keyboard = Robolectric.buildService(Keyboard2.class).create().get();
    try {
      EditorInfo info = new EditorInfo();
      info.inputType = InputType.TYPE_CLASS_TEXT;
      info.packageName = "test.editor";
      keyboard.onStartInput(info, false);
      keyboard.onStartInputView(info, false);

      Config config = Config.globalConfig();
      android.content.SharedPreferences protPrefs = DirectBootAwarePreferences.get_shared_preferences(keyboard);
      android.content.SharedPreferences defPrefs = android.preference.PreferenceManager.getDefaultSharedPreferences(keyboard);

      // Simulate selecting layout index 1 (e.g., Bangla layout)
      keyboard.setTextLayout(1);
      assertEquals(1, config.get_current_layout());
      assertEquals(1, protPrefs.getInt("current_layout_portrait", 0));

      // Simulate clipboard copy & dismissal on first keystroke selection update
      ClipboardHistoryService.setRecentCopiedClip("copied clip text");
      ClipboardHistoryService.dismissClip("copied clip text");
      DirectBootAwarePreferences.copy_preferences_to_protected_storage(keyboard, defPrefs);

      assertEquals("Bangla layout index must not reset to 0 after clipboard dismiss/preference sync",
          1, protPrefs.getInt("current_layout_portrait", 0));
      assertEquals("Active config layout must remain 1", 1, config.get_current_layout());
    } finally {
      keyboard.onDestroy();
    }
  }
}
