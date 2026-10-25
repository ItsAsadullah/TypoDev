package typodev.keyboard.ai;

import android.text.InputType;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import typodev.keyboard.suggestions.CandidatesView;

/** All reads and edits stay on the active, non-password target editor. */
public final class SlashCommandEditor {
  private SlashCommandEditor() {}
  public static boolean eligible(EditorInfo editor) {
    return editor != null && !CandidatesView.isPasswordField(editor)
        && (editor.inputType & InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT;
  }
  public static SlashCommand.Input read(InputConnection connection) {
    if (connection == null) return null;
    try {
      CharSequence selection = connection.getSelectedText(0);
      if (selection != null && selection.length() != 0) return null;
      SlashCommand.Input input = SlashCommand.parse(connection.getTextBeforeCursor(SlashCommand.MAX_CONTEXT + 1, 0));
      if (input == null) return null;
      CharSequence after = connection.getTextAfterCursor(1, 0);
      if (after != null && after.length() != 0 && after.charAt(0) != '\n' && after.charAt(0) != '\r') return null;
      return input;
    } catch (RuntimeException disconnected) { return null; }
  }
  public static boolean matches(InputConnection connection, SlashCommand.Input expected) {
    SlashCommand.Input current = read(connection);
    return current != null && expected != null && current.raw.equals(expected.raw);
  }
  public static boolean complete(InputConnection connection, SlashCommand.Input expected, SlashCommand command) {
    if (!matches(connection, expected) || !expected.suggestions().contains(command)) return false;
    connection.beginBatchEdit();
    try {
      connection.finishComposingText();
      if (!matches(connection, expected) || !connection.deleteSurroundingText(expected.raw.length(), 0)) return false;
      return connection.commitText(command.token() + " " + expected.argument, 1);
    } finally { connection.endBatchEdit(); }
  }
  public static boolean remove(InputConnection connection, SlashCommand.Input expected) {
    if (!matches(connection, expected)) return false;
    connection.beginBatchEdit();
    try {
      connection.finishComposingText();
      return matches(connection, expected) && connection.deleteSurroundingText(expected.raw.length(), 0);
    } finally { connection.endBatchEdit(); }
  }
}
