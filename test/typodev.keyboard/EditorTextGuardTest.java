package typodev.keyboard;

import android.view.inputmethod.InputConnection;
import java.lang.reflect.Proxy;
import org.junit.Test;
import static org.junit.Assert.*;
import typodev.keyboard.ai.EditorTextGuard;

public class EditorTextGuardTest
{
  private InputConnection editor(String before, String selection)
  {
    return (InputConnection)Proxy.newProxyInstance(InputConnection.class.getClassLoader(),
        new Class<?>[] { InputConnection.class }, (proxy, method, args) -> {
          if (method.getName().equals("getSelectedText")) return selection;
          if (method.getName().equals("getTextBeforeCursor"))
            return before == null ? null : before.substring(Math.max(0, before.length() - (Integer)args[0]));
          return null;
        });
  }

  @Test public void delayedCompletionRejectsChangedTextOrSelection()
  {
    assertTrue(EditorTextGuard.matches(editor("hello ", null), "hello ", false));
    assertFalse(EditorTextGuard.matches(editor("hello world", null), "hello ", false));
    assertFalse(EditorTextGuard.matches(editor("hello ", "selected"), "hello ", false));
    assertFalse(EditorTextGuard.matches(editor(null, null), "hello ", false));
  }

  @Test public void selectedTextMustStillMatch()
  {
    assertTrue(EditorTextGuard.matches(editor("prefix", "original"), "original", true));
    assertFalse(EditorTextGuard.matches(editor("prefix", "changed"), "original", true));
    assertFalse(EditorTextGuard.matches(editor("original", null), "original", true));
  }
}
