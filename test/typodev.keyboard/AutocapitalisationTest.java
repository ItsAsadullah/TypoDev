package typodev.keyboard;

import android.text.InputType;
import android.view.inputmethod.InputConnection;
import java.lang.reflect.Proxy;
import org.junit.Test;
import static org.junit.Assert.*;

public class AutocapitalisationTest
{
  private InputConnection failingConnection()
  {
    return (InputConnection) Proxy.newProxyInstance(
        InputConnection.class.getClassLoader(), new Class<?>[] {InputConnection.class},
        (proxy, method, args) -> { throw new IllegalStateException("Editor disconnected"); });
  }

  @Test
  public void testUnavailableCapsModeDoesNotCrashInputStart()
  {
    Config config = new Config();
    config.autocapitalisation = true;
    config.editor_config.caps_mode = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;
    config.editor_config.caps_initially_updated = true;
    config.editor_config.initial_sel_start = 42;
    boolean[] shift = {true};
    Autocapitalisation caps = new Autocapitalisation(null,
        (enable, disable) -> shift[0] = enable);

    caps.started(config, failingConnection());

    assertFalse(shift[0]);
    assertEquals(42, caps._cursor);
  }

  @Test
  public void testDisconnectedEditorDoesNotCrashSelectionUpdate()
  {
    Autocapitalisation caps = new Autocapitalisation(null, (enable, disable) -> {});
    caps._cursor = 8;
    caps._ic = failingConnection();
    caps.selection_updated(8, 0);
    assertEquals(0, caps._cursor);
    assertFalse(caps._should_update_caps_mode);
  }
}
