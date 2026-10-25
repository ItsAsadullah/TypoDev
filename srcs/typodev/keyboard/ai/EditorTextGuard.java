package typodev.keyboard.ai;

import android.view.inputmethod.InputConnection;

/** Prevent delayed transformations from replacing text that the user has edited. */
public final class EditorTextGuard
{
  private EditorTextGuard() {}

  public static boolean matches(InputConnection connection, String original, boolean hadSelection)
  {
    if (connection == null || original == null) return false;
    CharSequence selected = connection.getSelectedText(0);
    if (hadSelection)
      return selected != null && original.contentEquals(selected);
    if (selected != null && selected.length() > 0) return false;
    CharSequence before = connection.getTextBeforeCursor(original.length(), 0);
    return before != null && original.contentEquals(before);
  }
}
