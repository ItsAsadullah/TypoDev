package typodev.keyboard;

import android.view.inputmethod.InputConnection;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import typodev.keyboard.ai.AiWritingResultsView;
import typodev.keyboard.ai.SlashCommand;
import static org.junit.Assert.*;

/** Exercise the real insertion path without requiring Android view constructors. */
public class AiWritingInsertionTest {
  public static class Owner extends Keyboard2 {
    InputConnection editor;
    AiWritingResultsView panel;
    List<String> calls;
    @Override public InputConnection getCurrentInputConnection() { return editor; }
    @Override public long getInputSessionId() { return 7; }
    @Override public void closeWritingResults() {
      calls.add("close");
      panel.cancel();
    }
  }

  private static <T> T allocate(Class<T> type) throws Exception {
    Class<?> allocator = Class.forName("sun.misc.Unsafe");
    Field field = allocator.getDeclaredField("theUnsafe");
    field.setAccessible(true);
    return type.cast(allocator.getMethod("allocateInstance", Class.class).invoke(field.get(null), type));
  }

  private static void set(Object target, String fieldName, Object value) throws Exception {
    Field field = AiWritingResultsView.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  private List<String> insert(boolean commitSucceeds, boolean cancelDuringCommit) throws Exception {
    List<String> calls = new ArrayList<>();
    Owner owner = allocate(Owner.class);
    AiWritingResultsView panel = allocate(AiWritingResultsView.class);
    owner.panel = panel;
    owner.calls = calls;
    owner.editor = (InputConnection)Proxy.newProxyInstance(InputConnection.class.getClassLoader(),
        new Class<?>[] {InputConnection.class}, (proxy, method, args) -> {
          switch (method.getName()) {
            case "getTextBeforeCursor": return "original";
            case "getTextAfterCursor": return "";
            case "getSelectedText": return "";
            case "commitText":
              calls.add("commit:" + args[0]);
              if (cancelDuringCommit) panel.cancel();
              return commitSucceeds;
            default: calls.add(method.getName()); return true;
          }
        });
    set(panel, "keyboard", owner);
    set(panel, "connection", owner.editor);
    set(panel, "session", 7L);
    set(panel, "results", new ArrayList<>(Arrays.asList("revised")));
    set(panel, "source", "original");
    set(panel, "before", "original");
    set(panel, "after", "");
    set(panel, "selection", "");
    set(panel, "task", AiWritingResultsView.Task.REWRITE);
    Method insert = AiWritingResultsView.class.getDeclaredMethod("insert");
    insert.setAccessible(true);
    try { insert.invoke(panel); }
    catch (InvocationTargetException e) { throw new AssertionError("Insertion crashed", e.getCause()); }
    return calls;
  }

  @Test public void successfulTapEndsBatchBeforeClosingAndClearingConnection() throws Exception {
    assertEquals(Arrays.asList("beginBatchEdit", "finishComposingText", "deleteSurroundingText",
        "commit:revised", "endBatchEdit", "close"), insert(true, false));
  }

  @Test public void cancellationDuringCommitStillEndsBatchOnOriginalEditor() throws Exception {
    assertEquals(Arrays.asList("beginBatchEdit", "finishComposingText", "deleteSurroundingText",
        "commit:revised", "endBatchEdit"), insert(true, true));
  }

  @Test public void failedCommitEndsBatchAndKeepsPreviewOpen() throws Exception {
    assertEquals(Arrays.asList("beginBatchEdit", "finishComposingText", "deleteSurroundingText",
        "commit:revised", "endBatchEdit"), insert(false, false));
  }

  @Test public void commandResultReplacesOnlyCommandAndPreservesCodeFormatting() throws Exception {
    Owner owner = allocate(Owner.class);
    AiWritingResultsView panel = allocate(AiWritingResultsView.class);
    SlashCommandTest.Editor editor = new SlashCommandTest.Editor("Earlier text\n  /co Java JSON");
    String result = "```java\nJSONObject json = new JSONObject(input);\n```\nExplanation";
    owner.panel = panel; owner.calls = new ArrayList<>(); owner.editor = editor.connection;
    set(panel, "keyboard", owner); set(panel, "connection", editor.connection); set(panel, "session", 7L);
    set(panel, "results", new ArrayList<>(Arrays.asList(result)));
    set(panel, "source", "Java JSON"); set(panel, "before", editor.before);
    set(panel, "after", ""); set(panel, "selection", "");
    set(panel, "task", AiWritingResultsView.Task.COMMAND);
    set(panel, "command", SlashCommand.parse(editor.before).choose(SlashCommand.CODE));
    Method insert = AiWritingResultsView.class.getDeclaredMethod("insert"); insert.setAccessible(true); insert.invoke(panel);
    assertEquals("Earlier text\n  " + result, editor.before);
    assertEquals(Arrays.asList("begin", "finish", "delete:13", "commit:" + result, "end"), editor.edits);
    assertEquals(Arrays.asList("close"), owner.calls);
  }

  @Test public void commandChangedDuringCompositionDoesNotDeleteOrInsert() throws Exception {
    Owner owner = allocate(Owner.class);
    AiWritingResultsView panel = allocate(AiWritingResultsView.class);
    SlashCommandTest.Editor editor = new SlashCommandTest.Editor("/reply hello");
    owner.panel = panel; owner.calls = new ArrayList<>(); owner.editor = editor.connection;
    set(panel, "keyboard", owner); set(panel, "connection", editor.connection); set(panel, "session", 7L);
    set(panel, "results", new ArrayList<>(Arrays.asList("polite reply")));
    set(panel, "source", "hello"); set(panel, "before", editor.before);
    set(panel, "after", ""); set(panel, "selection", "");
    set(panel, "task", AiWritingResultsView.Task.COMMAND); set(panel, "command", SlashCommand.parse(editor.before));
    editor.changeOnFinish = true;
    Method insert = AiWritingResultsView.class.getDeclaredMethod("insert"); insert.setAccessible(true); insert.invoke(panel);
    assertEquals("/reply hello changed", editor.before);
    assertEquals(Arrays.asList("begin", "finish", "end"), editor.edits); assertTrue(owner.calls.isEmpty());
  }
}
