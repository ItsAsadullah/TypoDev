package typodev.keyboard;

import android.text.Editable;
import android.text.Selection;
import android.view.KeyEvent;
import android.view.inputmethod.BaseInputConnection;
import android.widget.EditText;

/**
 * Shared InputConnection for in-keyboard EditText bars (AI Prompt, Emoji Search,
 * Clipboard Search, Clipboard Add). Properly supports IME composing spans
 * (e.g. Avro Phonetic), Unicode surrogate-pair safe deletion, and Enter/IME actions.
 */
public class InKeyboardEditTextConnection extends BaseInputConnection
{
  private final EditText _editText;
  private final Runnable _onSubmitAction;

  public InKeyboardEditTextConnection(EditText editText, Runnable onSubmitAction)
  {
    super(editText, true);
    _editText = editText;
    _onSubmitAction = onSubmitAction;
  }

  @Override
  public Editable getEditable()
  {
    return _editText != null ? _editText.getText() : null;
  }

  private boolean replaceTextInternal(CharSequence text, int newCursorPosition, boolean composing)
  {
    if (text == null) return true;
    Editable content = getEditable();
    if (content == null) return false;

    beginBatchEdit();
    try
    {
      int a = BaseInputConnection.getComposingSpanStart(content);
      int b = BaseInputConnection.getComposingSpanEnd(content);
      if (b < a)
      {
        int tmp = a;
        a = b;
        b = tmp;
      }
      if (a != -1 && b != -1)
      {
        BaseInputConnection.removeComposingSpans(content);
      }
      else
      {
        a = Selection.getSelectionStart(content);
        b = Selection.getSelectionEnd(content);
        if (a < 0) a = content.length();
        if (b < 0) b = content.length();
        if (b < a)
        {
          int tmp = a;
          a = b;
          b = tmp;
        }
      }

      content.replace(a, b, text);

      int newCursor = (newCursorPosition > 0)
          ? (a + text.length() + newCursorPosition - 1)
          : (a + newCursorPosition);
      newCursor = Math.max(0, Math.min(newCursor, content.length()));
      _editText.setSelection(newCursor);

      if (composing && text.length() > 0)
      {
        setComposingRegion(a, Math.min(a + text.length(), content.length()));
      }
      return true;
    }
    finally
    {
      endBatchEdit();
    }
  }

  @Override
  public boolean commitText(CharSequence text, int newCursorPosition)
  {
    return replaceTextInternal(text, newCursorPosition, false);
  }

  @Override
  public boolean setComposingText(CharSequence text, int newCursorPosition)
  {
    return replaceTextInternal(text, newCursorPosition, true);
  }

  @Override
  public boolean finishComposingText()
  {
    Editable content = getEditable();
    if (content != null)
    {
      BaseInputConnection.removeComposingSpans(content);
    }
    return true;
  }

  @Override
  public boolean deleteSurroundingText(int beforeLength, int afterLength)
  {
    Editable content = getEditable();
    if (content == null) return false;

    beginBatchEdit();
    try
    {
      int start = Selection.getSelectionStart(content);
      int end = Selection.getSelectionEnd(content);
      if (start < 0) start = content.length();
      if (end < 0) end = content.length();
      if (start > end)
      {
        int tmp = start;
        start = end;
        end = tmp;
      }

      if (start != end)
      {
        content.delete(start, end);
        _editText.setSelection(Math.min(start, content.length()));
        return true;
      }

      int compStart = BaseInputConnection.getComposingSpanStart(content);
      int compEnd = BaseInputConnection.getComposingSpanEnd(content);
      if (compEnd < compStart)
      {
        int tmp = compStart;
        compStart = compEnd;
        compEnd = tmp;
      }

      int beforeAnchor = (compStart != -1 && compEnd != -1 && compStart <= start) ? compStart : start;
      int afterAnchor = (compStart != -1 && compEnd != -1 && compEnd >= end) ? compEnd : end;

      int delBeforeStart = Math.max(0, beforeAnchor - Math.max(0, beforeLength));
      if (delBeforeStart > 0 && delBeforeStart < content.length()
          && Character.isLowSurrogate(content.charAt(delBeforeStart))
          && Character.isHighSurrogate(content.charAt(delBeforeStart - 1)))
      {
        delBeforeStart--;
      }

      int delAfterEnd = Math.min(content.length(), afterAnchor + Math.max(0, afterLength));
      if (delAfterEnd > 0 && delAfterEnd < content.length()
          && Character.isLowSurrogate(content.charAt(delAfterEnd))
          && Character.isHighSurrogate(content.charAt(delAfterEnd - 1)))
      {
        delAfterEnd++;
      }

      if (afterAnchor < delAfterEnd)
      {
        content.delete(afterAnchor, delAfterEnd);
      }
      int deletedBefore = 0;
      if (delBeforeStart < beforeAnchor)
      {
        deletedBefore = beforeAnchor - delBeforeStart;
        content.delete(delBeforeStart, beforeAnchor);
      }
      int newSel = Math.max(0, Math.min(start - deletedBefore, content.length()));
      _editText.setSelection(newSel);
      return true;
    }
    finally
    {
      endBatchEdit();
    }
  }

  private boolean deleteBackwardCodePoint()
  {
    Editable content = getEditable();
    if (content == null) return false;

    int start = Selection.getSelectionStart(content);
    int end = Selection.getSelectionEnd(content);
    if (start < 0) start = content.length();
    if (end < 0) end = content.length();
    if (start > end)
    {
      int tmp = start;
      start = end;
      end = tmp;
    }

    if (start != end)
    {
      content.delete(start, end);
      _editText.setSelection(Math.min(start, content.length()));
      return true;
    }
    if (start <= 0) return true;

    int delStart;
    try
    {
      delStart = Character.offsetByCodePoints(content, start, -1);
    }
    catch (IndexOutOfBoundsException e)
    {
      delStart = Math.max(0, start - 1);
    }
    content.delete(delStart, start);
    _editText.setSelection(Math.min(delStart, content.length()));
    return true;
  }

  @Override
  public boolean performEditorAction(int actionCode)
  {
    finishComposingText();
    if (_onSubmitAction != null)
    {
      _onSubmitAction.run();
    }
    return true;
  }

  @Override
  public boolean sendKeyEvent(KeyEvent event)
  {
    if (event != null && event.getAction() == KeyEvent.ACTION_DOWN)
    {
      int code = event.getKeyCode();
      if (code == KeyEvent.KEYCODE_DEL)
      {
        return deleteBackwardCodePoint();
      }
      if (code == KeyEvent.KEYCODE_ENTER || code == KeyEvent.KEYCODE_NUMPAD_ENTER)
      {
        return performEditorAction(0);
      }
    }
    return super.sendKeyEvent(event);
  }

  @Override
  public CharSequence getTextBeforeCursor(int n, int flags)
  {
    Editable content = getEditable();
    if (content == null || n <= 0) return "";
    int start = Selection.getSelectionStart(content);
    int end = Selection.getSelectionEnd(content);
    if (start < 0) start = content.length();
    if (end < 0) end = content.length();
    int min = Math.min(start, end);
    if (min <= 0) return "";
    int from = Math.max(0, min - n);
    return content.subSequence(from, min);
  }

  @Override
  public CharSequence getTextAfterCursor(int n, int flags)
  {
    Editable content = getEditable();
    if (content == null || n <= 0) return "";
    int start = Selection.getSelectionStart(content);
    int end = Selection.getSelectionEnd(content);
    if (start < 0) start = content.length();
    if (end < 0) end = content.length();
    int max = Math.max(start, end);
    if (max < 0 || max >= content.length()) return "";
    int to = Math.min(content.length(), max + n);
    return content.subSequence(max, to);
  }

  @Override
  public CharSequence getSelectedText(int flags)
  {
    Editable content = getEditable();
    if (content == null) return "";
    int start = Selection.getSelectionStart(content);
    int end = Selection.getSelectionEnd(content);
    if (start < 0 || end < 0 || start == end) return "";
    if (start > end)
    {
      int tmp = start;
      start = end;
      end = tmp;
    }
    return content.subSequence(start, Math.min(end, content.length()));
  }

  @Override
  public boolean setSelection(int start, int end)
  {
    Editable content = getEditable();
    if (content == null) return false;
    int len = content.length();
    int s = Math.max(0, Math.min(start, len));
    int e = Math.max(0, Math.min(end, len));
    _editText.setSelection(s, e);
    return true;
  }

  @Override
  public boolean performContextMenuAction(int id)
  {
    return _editText != null && _editText.onTextContextMenuItem(id);
  }
}
