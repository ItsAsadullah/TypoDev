package typodev.keyboard;

import android.app.Activity;
import android.preference.PreferenceManager;
import android.view.inputmethod.InputConnection;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import typodev.keyboard.ai.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class AiProviderRoutingTest {
  public static class Owner extends Keyboard2 {
    InputConnection connection;
    @Override public InputConnection getCurrentInputConnection() { return connection; }
    @Override public long getInputSessionId() { return 7L; }
    @Override public int getColorLabel() { return 0xff000000; }
    @Override public int getColorKey() { return 0xffdddddd; }
    @Override public int getColorKeyActivated() { return 0xff0000ff; }
    @Override public int getColorKeyboard() { return 0xffeeeeee; }
    @Override public int getBottomSafety() { return 0; }
  }
  private Activity activity;
  private Field singleton;
  private Object previous;
  private String system, input;
  @Before public void setup() throws Exception {
    activity = Robolectric.buildActivity(Activity.class).setup().get();
    AiProvider.Manager.setActiveProviderType(activity, AiProvider.ProviderType.OPENAI_COMPATIBLE);
    singleton = OpenAiCompatibleProvider.class.getDeclaredField("_instance"); singleton.setAccessible(true);
    previous = singleton.get(null);
    singleton.set(null, new OpenAiCompatibleProvider() {
      @Override public void generate(android.content.Context c, String prompt, String text, AiProvider.Callback callback) {
        system = prompt; input = text;
      }
    });
  }
  @After public void cleanup() throws Exception { singleton.set(null, previous); }
  @Test public void rewriteUsesSelectedProviderWithoutGeminiKey() {
    Owner owner = Robolectric.buildService(Owner.class).get();
    owner.connection = (InputConnection)Proxy.newProxyInstance(InputConnection.class.getClassLoader(),
        new Class<?>[]{InputConnection.class}, (proxy, method, args) -> {
          if (method.getName().equals("getTextBeforeCursor")) return "hello";
          if (method.getReturnType() == boolean.class) return true;
          return null;
        });
    AiWritingResultsView panel = new AiWritingResultsView(activity, owner);
    panel.start("hello", null, "প্রুফরিড", AiWritingResultsView.Task.REWRITE);
    assertNotNull(system); assertTrue(system.contains("SOURCE:\nhello")); assertEquals("", input);
  }
  @Test public void invalidTemperatureCannotCreateInvalidJsonRequest() {
    PreferenceManager.getDefaultSharedPreferences(activity).edit().putString(AiProvider.PREF_AI_TEMPERATURE, "NaN").commit();
    assertEquals(.7f, AiProvider.Manager.getTemperature(activity), 0f);
    AiProvider.Manager.setTemperature(activity, Float.POSITIVE_INFINITY);
    assertEquals(.7f, AiProvider.Manager.getTemperature(activity), 0f);
    AiProvider.Manager.setTemperature(activity, -10f); assertEquals(0f, AiProvider.Manager.getTemperature(activity), 0f);
    AiProvider.Manager.setTemperature(activity, 10f); assertEquals(2f, AiProvider.Manager.getTemperature(activity), 0f);
  }
}
