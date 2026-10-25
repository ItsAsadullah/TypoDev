package typodev.keyboard;

import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class PointerFeedbackTest {
  Keyboard2 keyboard;
  Pointers pointers;
  KeyboardData.Key key;
  Recorder recorder;

  static class Recorder implements Pointers.IPointerEventHandler {
    int feedback, flagVibrations, selections;
    List<KeyValue> typed = new ArrayList<>();
    @Override public KeyValue modifyKey(KeyValue value, Pointers.Modifiers mods) { return value; }
    @Override public void onPointerDown(KeyValue value, boolean swipe) { selections++; }
    @Override public void onPointerDown(KeyboardData.Key key, KeyValue value, boolean swipe) { selections++; }
    @Override public void onPointerFeedback(KeyValue value) { feedback++; }
    @Override public void onPointerUp(KeyValue value, Pointers.Modifiers mods) { typed.add(value); }
    @Override public void onPointerFlagsChanged(boolean vibrate) { if (vibrate) flagVibrations++; }
    @Override public void onPointerHold(KeyValue value, Pointers.Modifiers mods) { typed.add(value); }
  }

  @Before public void setup() {
    keyboard = Robolectric.buildService(Keyboard2.class).create().get();
    typodev.keyboard.Config config = typodev.keyboard.Config.globalConfig();
    config.swipe_dist_px = 20;
    recorder = new Recorder();
    pointers = new Pointers(recorder, config);
    KeyValue[] values = new KeyValue[9];
    for (int i = 0; i < values.length; i++) values[i] = KeyValue.getKeyByName(String.valueOf((char)('a' + i)));
    key = new KeyboardData.Key(values, KeyValue.getKeyByName("z"), 0, 1f, 0f, null, KeyboardData.Key.Role.Normal);
  }

  @After public void cleanup() { pointers.clear(); keyboard.onDestroy(); }

  @Test public void allFourCornersGiveOneFeedbackAndTypeTheSelectedCharacter() {
    int[][] corners = {{40,40,1}, {160,40,2}, {40,160,3}, {160,160,4}};
    for (int[] corner : corners) {
      int feedbackBefore = recorder.feedback;
      pointers.onTouchDown(100, 100, 0, key);
      pointers.onTouchMove(corner[0], corner[1], 0);
      pointers.onTouchMove(corner[0], corner[1], 0);
      pointers.onTouchUp(0);
      assertEquals(1, recorder.feedback - feedbackBefore);
      assertEquals(key.keys[corner[2]], recorder.typed.get(recorder.typed.size() - 1));
    }
    assertEquals(0, recorder.flagVibrations);
    assertEquals(4, recorder.typed.size());
    assertEquals(8, recorder.selections); // Center and corner previews still update.
  }

  @Test public void returningToCenterOrRotatingDoesNotReplayFeedback() {
    pointers.onTouchDown(100, 100, 0, key);
    pointers.onTouchMove(160, 40, 0);
    pointers.onTouchMove(100, 100, 0);
    pointers.onTouchUp(0);
    assertEquals(1, recorder.feedback);
    pointers.onTouchDown(100, 100, 0, key);
    pointers.onTouchMove(160, 40, 0);
    pointers.onTouchMove(160, 160, 0);
    pointers.onTouchMove(40, 160, 0);
    pointers.onTouchUp(0);
    assertEquals(2, recorder.feedback);
    assertEquals(0, recorder.flagVibrations);
    assertEquals(2, recorder.typed.size());
  }

  @Test public void quickTapsAndTwoFingersEachKeepTheirOwnFeedback() {
    pointers.onTouchDown(100, 100, 0, key);
    pointers.onTouchUp(0);
    pointers.onTouchDown(100, 100, 0, key);
    pointers.onTouchDown(100, 100, 1, key);
    pointers.onTouchMove(160, 40, 1);
    pointers.onTouchUp(1);
    pointers.onTouchUp(0);
    assertEquals(3, recorder.feedback);
    assertEquals(3, recorder.typed.size());
    assertEquals(key.keys[0], recorder.typed.get(0));
    assertEquals(key.keys[2], recorder.typed.get(1));
    assertEquals(key.keys[0], recorder.typed.get(2));
  }

  @Test public void enabledCornerFeedbackConfirmsEachCornerExactlyOnce() {
    typodev.keyboard.Config.globalConfig().corner_feedback_enabled = true;
    for (int[] corner : new int[][]{{40,40,1}, {160,40,2}, {40,160,3}, {160,160,4}}) {
      int previous = recorder.feedback;
      pointers.onTouchDown(100, 100, 0, key);
      pointers.onTouchMove(corner[0], corner[1], 0);
      pointers.onTouchMove(corner[0] + 1, corner[1] + 1, 0);
      pointers.onTouchUp(0);
      assertEquals(2, recorder.feedback - previous);
      assertEquals(key.keys[corner[2]], recorder.typed.get(recorder.typed.size() - 1));
    }
    assertEquals(0, recorder.flagVibrations);
    int previous = recorder.feedback;
    pointers.onTouchDown(100, 100, 0, key);
    pointers.onTouchUp(0);
    assertEquals(1, recorder.feedback - previous);
  }

  @Test public void emptyCornerDoesNotGiveFalseSelectionConfirmation() {
    typodev.keyboard.Config.globalConfig().corner_feedback_enabled = true;
    KeyValue[] values = new KeyValue[9];
    values[0] = KeyValue.getKeyByName("a");
    KeyboardData.Key plain = new KeyboardData.Key(values, null, 0, 1f, 0f, null, KeyboardData.Key.Role.Normal);
    pointers.onTouchDown(100, 100, 0, plain);
    pointers.onTouchMove(160, 40, 0);
    pointers.onTouchUp(0);
    assertEquals(1, recorder.feedback);
    assertEquals(values[0], recorder.typed.get(0));
  }

  @Test public void savedPreferenceCanEnableAndDisableExtraFeedback() {
    android.content.SharedPreferences prefs = DirectBootAwarePreferences.get_shared_preferences(keyboard);
    assertFalse(typodev.keyboard.Config.globalConfig().corner_feedback_enabled);
    for (boolean enabled : new boolean[]{true, false}) {
      prefs.edit().putBoolean("corner_feedback_enabled", enabled).commit();
      keyboard.onSharedPreferenceChanged(prefs, "corner_feedback_enabled");
      assertEquals(enabled, typodev.keyboard.Config.globalConfig().corner_feedback_enabled);
    }
  }
}
