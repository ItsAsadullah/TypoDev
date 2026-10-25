package typodev.keyboard;

import android.text.InputType;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.os.Handler;
import android.view.KeyEvent;
import typodev.keyboard.suggestions.Suggestions;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import typodev.keyboard.ai.SlashCommand;
import typodev.keyboard.ai.SlashCommandEditor;
import typodev.keyboard.ai.AiWritingResultsView;
import static org.junit.Assert.*;

public class SlashCommandTest {
  @Test public void bareSlashOffersAllSixCommands() {
    assertEquals(Arrays.asList(SlashCommand.values()), SlashCommand.parse("/").suggestions());
  }
  @Test public void prefixFiltersAndCompletionPreservesOriginalFragment() {
    SlashCommand.Input input = SlashCommand.parse("Earlier text\n  /RE hello");
    assertEquals(Arrays.asList(SlashCommand.REWRITE, SlashCommand.REPLY, SlashCommand.REMEMBER), input.suggestions());
    assertNull(input.command);
    assertEquals("/RE hello", input.choose(SlashCommand.REPLY).raw);
    assertEquals("hello", input.choose(SlashCommand.REPLY).argument);
    assertEquals(SlashCommand.REPLY, input.choose(SlashCommand.REPLY).command);
  }
  @Test public void bengaliArgumentsAndLeadingSpacesArePreserved() {
    SlashCommand.Input input = SlashCommand.parse(" \t/remember আগামীকাল সকাল ১০টায় মিটিং ");
    assertEquals("আগামীকাল সকাল ১০টায় মিটিং", input.argument);
    assertEquals(SlashCommand.REMEMBER, input.command);
    assertTrue(input.hasDelimiter);
    assertFalse(SlashCommand.parse("/rewrite").hasDelimiter);
  }
  @Test public void ordinarySlashesUrlsPathsAndUnknownCommandsDoNotTrigger() {
    for (String value : Arrays.asList("https://example.com", "abc /reply hi", "/reply/file", "/reply.md",
        "//comment", "/tmp", "/42", "", "/unknown", "/code_example")) assertNull(value, SlashCommand.parse(value));
  }
  @Test public void truncatedOrOversizedCommandIsRejected() {
    char[] padding = new char[SlashCommand.MAX_CONTEXT]; Arrays.fill(padding, 'x');
    assertNull(SlashCommand.parse("/reply " + new String(padding)));
    assertNull(SlashCommand.parse("/" + new String(padding)));
    assertNotNull(SlashCommand.parse(new String(padding) + "\n/reply hi"));
  }
  @Test(expected = IllegalArgumentException.class) public void mismatchedCommandCannotBeChosen() {
    SlashCommand.parse("/em").choose(SlashCommand.CODE);
  }
  @Test public void onlyNonPasswordTextEditorsAreEligible() {
    EditorInfo editor = new EditorInfo();
    editor.inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE;
    assertTrue(SlashCommandEditor.eligible(editor));
    for (int type : new int[] {InputType.TYPE_CLASS_NUMBER, InputType.TYPE_CLASS_PHONE,
        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD,
        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD}) {
      editor.inputType = type; assertFalse(SlashCommandEditor.eligible(editor));
    }
    assertFalse(SlashCommandEditor.eligible(null));
  }
  @Test public void commandCompletionEditsOnlyCurrentCommandLeavingEarlierText() {
    Editor editor = new Editor("Previous message\n  /re");
    assertTrue(SlashCommandEditor.complete(editor.connection, SlashCommandEditor.read(editor.connection), SlashCommand.REPLY));
    assertEquals("Previous message\n  /reply ", editor.before);
    assertEquals(Arrays.asList("begin", "finish", "delete:3", "commit:/reply ", "end"), editor.edits);
  }
  @Test public void completionKeepsSuppliedArguments() {
    Editor editor = new Editor("/co Java JSON");
    assertTrue(SlashCommandEditor.complete(editor.connection, SlashCommandEditor.read(editor.connection), SlashCommand.CODE));
    assertEquals("/code Java JSON", editor.before);
  }
  @Test public void staleInputCannotDeleteNewText() {
    Editor editor = new Editor("/remember hello");
    SlashCommand.Input input = SlashCommandEditor.read(editor.connection);
    editor.before = "/remember edited";
    assertFalse(SlashCommandEditor.remove(editor.connection, input));
    assertFalse(SlashCommandEditor.complete(editor.connection, input, SlashCommand.REMEMBER));
    assertTrue(editor.edits.isEmpty());
  }
  @Test public void cursorInsideTextOrSelectionSuppressesCommands() {
    Editor editor = new Editor("/reply");
    editor.after = " more text"; assertNull(SlashCommandEditor.read(editor.connection));
    editor.after = "\nnext line"; assertNotNull(SlashCommandEditor.read(editor.connection));
    editor.selected = "selected"; assertNull(SlashCommandEditor.read(editor.connection));
  }
  @Test public void changedCompositionCannotBeDeletedAndBatchAlwaysEnds() {
    Editor editor = new Editor("/reply hi");
    SlashCommand.Input input = SlashCommandEditor.read(editor.connection);
    editor.changeOnFinish = true;
    assertFalse(SlashCommandEditor.remove(editor.connection, input));
    assertEquals(Arrays.asList("begin", "finish", "end"), editor.edits);
  }
  @Test public void rememberRemovalPreservesSurroundingMessages() {
    Editor editor = new Editor("previous\n  /remember meeting");
    editor.after = "\nnext";
    assertTrue(SlashCommandEditor.remove(editor.connection, SlashCommandEditor.read(editor.connection)));
    assertEquals("previous\n  ", editor.before); assertEquals("\nnext", editor.after);
  }
  @Test public void promptsDescribeDistinctTasksWithoutIncludingUserData() {
    assertTrue(SlashCommand.REWRITE.systemPrompt().contains("fluent English"));
    assertTrue(SlashCommand.REPLY.systemPrompt().contains("polite, respectful reply"));
    assertTrue(SlashCommand.EMAIL.systemPrompt().contains("Subject and body"));
    assertTrue(SlashCommand.CODE.systemPrompt().contains("Java if none"));
    assertTrue(SlashCommand.EXPLAIN.systemPrompt().contains("possible causes"));
    for (SlashCommand command : SlashCommand.values()) if (command != SlashCommand.REMEMBER) {
      assertTrue(command.systemPrompt().contains("source data"));
      assertTrue(command.systemPrompt().contains("one complete result"));
    }
  }
  @Test(expected = IllegalStateException.class) public void notesCannotBeDispatchedToAI() {
    SlashCommand.REMEMBER.systemPrompt();
  }
  @Test public void codeAndEmailResponsesKeepLineBreaksNumberingAndEntireOutput() {
    String code = "```java\nimport org.json.JSONObject;\nJSONObject value = new JSONObject(json);\n```\n1. Read the object\n2. Check missing keys";
    assertEquals(Arrays.asList(code), AiWritingResultsView.parseResults(AiWritingResultsView.Task.COMMAND, code));
    String email = "Subject: Meeting\n\nDear team,\nPlease attend tomorrow.\n\nBest regards";
    assertEquals(Arrays.asList(email), AiWritingResultsView.parseResults(AiWritingResultsView.Task.COMMAND, email));
    assertTrue(AiWritingResultsView.parseResults(AiWritingResultsView.Task.COMMAND, " \n ").isEmpty());
    assertTrue(AiWritingResultsView.parseResults(AiWritingResultsView.Task.COMMAND, null).isEmpty());
  }

  @Test public void commandTokenBypassesAvroAndSpaceAutocorrection() {
    Editor editor = new Editor("");
    Config config = new Config(); config.is_avro_mode = true; config.space_bar_auto_complete = true;
    KeyEventHandler.IReceiver receiver = receiver(editor);
    KeyEventHandler handler = new KeyEventHandler(receiver, new Suggestions(receiver, config, null));
    handler.started(config);
    handler.send_text("/");
    for (char letter : "reply".toCharArray()) handler.send_text(String.valueOf(letter));
    assertEquals("/reply", editor.before); assertFalse(handler.isAvroComposingActive());
    handler.handle_space_bar();
    assertEquals("/reply ", editor.before); assertTrue(handler.isAvroActive());
  }
  @Test public void enterIsInterceptedOnlyWhenCommandIsReadyAndHasNoModifiers() throws Exception {
    Editor editor = new Editor("/reply hello");
    Config config = new Config();
    KeyEventHandler.IReceiver receiver = receiver(editor);
    KeyEventHandler handler = new KeyEventHandler(receiver, new Suggestions(receiver, config, null));
    handler.started(config);
    // A refresh is already queued: this JVM test has no Android Looper to schedule one.
    java.lang.reflect.Field queued = CurrentlyTypedWord.class.getDeclaredField("_refresh_pending");
    queued.setAccessible(true); queued.setBoolean(handler._typedword, true);
    handler.send_key_down_up(KeyEvent.KEYCODE_ENTER, 0);
    assertTrue(editor.edits.isEmpty());
    handler.send_key_down_up(KeyEvent.KEYCODE_ENTER, KeyEvent.META_SHIFT_ON);
    assertEquals(Arrays.asList("key", "key"), editor.edits);
    editor.edits.clear(); editor.before = "ordinary text";
    handler.send_key_down_up(KeyEvent.KEYCODE_ENTER, 0);
    assertEquals(Arrays.asList("key", "key"), editor.edits);
  }

  private KeyEventHandler.IReceiver receiver(Editor editor) {
    return new KeyEventHandler.IReceiver() {
      @Override public void handle_event_key(KeyValue.Event ev) {}
      @Override public void set_shift_state(boolean state, boolean lock) {}
      @Override public void set_compose_pending(boolean pending) {}
      @Override public void selection_state_changed(boolean ongoing) {}
      @Override public InputConnection getCurrentInputConnection() { return editor.connection; }
      @Override public Handler getHandler() { return null; }
      @Override public void set_suggestions(Suggestions suggestions) {}
      @Override public CharSequence getTextBeforeCursor(int n, int flags) { return editor.before; }
      @Override public boolean isSelectionActive() { return !editor.selected.isEmpty(); }
      @Override public boolean isSlashCommandTokenActive() {
        SlashCommand.Input input = SlashCommand.parse(editor.before); return input != null && !input.hasDelimiter;
      }
      @Override public boolean runSlashCommand() {
        SlashCommand.Input input = SlashCommand.parse(editor.before);
        return input != null && input.command != null && !input.argument.isEmpty();
      }
    };
  }

  public static final class Editor {
    public String before, after = "", selected = "";
    public boolean changeOnFinish;
    public final List<String> edits = new ArrayList<>();
    public final InputConnection connection;
    public Editor(String text) {
      before = text;
      connection = (InputConnection)Proxy.newProxyInstance(InputConnection.class.getClassLoader(),
          new Class<?>[] {InputConnection.class}, (proxy, method, args) -> {
            switch (method.getName()) {
              case "getTextBeforeCursor": return before.substring(Math.max(0, before.length() - (Integer)args[0]));
              case "getTextAfterCursor": return after.substring(0, Math.min(after.length(), (Integer)args[0]));
              case "getSelectedText": return selected;
              case "beginBatchEdit": edits.add("begin"); return true;
              case "endBatchEdit": edits.add("end"); return true;
              case "finishComposingText": edits.add("finish"); if (changeOnFinish) before += " changed"; return true;
              case "deleteSurroundingText": edits.add("delete:" + args[0]); before = before.substring(0, before.length() - (Integer)args[0]); return true;
              case "commitText": edits.add("commit:" + args[0]); before += args[0]; return true;
              case "sendKeyEvent": edits.add("key"); return true;
              default: return true;
            }
          });
    }
  }
}
