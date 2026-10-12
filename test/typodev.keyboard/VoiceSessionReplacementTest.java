package typodev.keyboard;

import android.view.inputmethod.InputConnection;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import org.junit.Test;
import static org.junit.Assert.*;

public class VoiceSessionReplacementTest {
  public static class Owner extends Keyboard2 {
    InputConnection editor;
    @Override public InputConnection getCurrentInputConnection() { return editor; }
  }

  private Owner owner(String oldText, String before, boolean deleteSucceeds,
      String selected, StringBuilder committed, int[] ended) throws Exception {
    Class<?> allocator = Class.forName("sun.misc.Unsafe");
    Field field = allocator.getDeclaredField("theUnsafe"); field.setAccessible(true);
    Owner owner = (Owner)allocator.getMethod("allocateInstance", Class.class).invoke(field.get(null), Owner.class);
    owner.editor = (InputConnection)Proxy.newProxyInstance(InputConnection.class.getClassLoader(),
        new Class<?>[] {InputConnection.class}, (proxy, method, args) -> {
          switch (method.getName()) {
            case "getSelectedText": return selected;
            case "getTextBeforeCursor": return before.substring(Math.max(0, before.length() - (Integer)args[0]));
            case "deleteSurroundingText":
              assertEquals(oldText.length() + 1, args[0]); return deleteSucceeds;
            case "commitText": committed.append(args[0]); return true;
            case "endBatchEdit": ended[0]++; return true;
            default: return true;
          }
        });
    return owner;
  }

  @Test public void punctuatesMoreThanFiveHundredCharacters() throws Exception {
    StringBuilder source = new StringBuilder();
    for (int i = 0; i < 120; i++) source.append("আমি ভালো আছি ");
    String raw = source.toString().trim();
    StringBuilder committed = new StringBuilder(); int[] ended = {0};
    Owner owner = owner(raw, "আগের লেখা " + raw + " ", true, "", committed, ended);
    assertTrue(owner.replaceVoiceSessionText(raw, raw + "।"));
    assertEquals(raw + "। ", committed.toString());
    assertEquals(1, ended[0]);
  }

  @Test public void failedDeleteDoesNotAppendADuplicateTranscript() throws Exception {
    StringBuilder committed = new StringBuilder(); int[] ended = {0};
    Owner owner = owner("আমি ভালো আছি", "আমি ভালো আছি ", false, "", committed, ended);
    assertFalse(owner.replaceVoiceSessionText("আমি ভালো আছি", "আমি ভালো আছি।"));
    assertEquals("", committed.toString()); assertEquals(1, ended[0]);
  }

  @Test public void editsOrSelectionsPreventReplacingExistingText() throws Exception {
    StringBuilder committed = new StringBuilder(); int[] ended = {0};
    Owner edited = owner("আগের কথা", "আগের কথা এখন বদলেছে", true, "", committed, ended);
    assertFalse(edited.replaceVoiceSessionText("আগের কথা", "আগের কথা।"));
    Owner selected = owner("আগের কথা", "আগের কথা ", true, "নতুন সিলেকশন", committed, ended);
    assertFalse(selected.replaceVoiceSessionText("আগের কথা", "আগের কথা।"));
    assertEquals("", committed.toString()); assertEquals(2, ended[0]);
  }
}
