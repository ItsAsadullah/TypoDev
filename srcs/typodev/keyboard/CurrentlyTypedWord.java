package typodev.keyboard;

import android.annotation.TargetApi;
import android.os.Build.VERSION;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.SurroundingText;

/** Keep track of the word being typed. This also tracks whether the selection
    is empty. */
public final class CurrentlyTypedWord
{
  InputConnection _ic = null;
  Handler _handler;
  Callback _callback;

  /** The currently typed word. */
  StringBuilder _w = new StringBuilder();
  /** The last completed word before space/punctuation. */
  String _last_completed_word = "";
  /** This can be disabled if the editor doesn't support looking at the text
      before the cursor. */
  boolean _enabled = false;
  /** The current word is empty while the selection is ongoing. */
  boolean _has_selection = false;
  /** Used to avoid concurrent refreshes in [delayed_refresh()]. */
  boolean _refresh_pending = false;

  /** The estimated cursor position in UTF-16 chars, like InputConnection. Used to avoid expensive IPC
      calls when the typed word can be estimated locally with [typed]. When the
      cursor position gets out of sync, the text before the cursor is queried
      again to the editor. */
  int _cursor;
  /** The cursor position within the current word relative to the end of the
      word in chars. Equal to [0] when the cursor is at the end of the word. */
  int _w_cursor;

  public CurrentlyTypedWord(Handler h, Callback cb)
  {
    _handler = h;
    _callback = cb;
  }

  public String get()
  {
    return _w.toString();
  }

  public String get_last_completed_word()
  {
    return _last_completed_word;
  }

  public boolean is_selection_not_empty()
  {
    return _has_selection;
  }

  /** The cursor position relative to the end of the word. */
  public int cursor_relative()
  {
    return _w_cursor;
  }

  public void started(Config conf, InputConnection ic)
  {
    _refresh_pending = false;
    if (_handler != null)
      _handler.removeCallbacks(delayed_refresh_run);
    _ic = ic;
    _enabled = true;
    _w.setLength(0);
    _last_completed_word = "";
    EditorConfig e = conf.editor_config;
    _has_selection = e.initial_sel_start != e.initial_sel_end;
    _cursor = e.initial_sel_start;
    _w_cursor = 0;
    if (!_has_selection)
    {
      set_current_word(e.initial_text_before_cursor);
      _w_cursor = (e.initial_text_after_cursor == null) ? 0 :
        -append_chars(e.initial_text_after_cursor); 
    }
    callback();
  }

  public void typed(String s)
  {
    if (!_enabled)
      return;
    _has_selection = false;
    type_chars(s);
    callback();
  }

  public void finished()
  {
    _enabled = false;
    _refresh_pending = false;
    if (_handler != null)
      _handler.removeCallbacks(delayed_refresh_run);
    _ic = null;
    _w.setLength(0);
    _last_completed_word = "";
    _has_selection = false;
    _w_cursor = 0;
  }

  public void selection_updated(int oldSelStart, int newSelStart, int newSelEnd)
  {
    // Avoid the expensive [refresh_current_word] call when [typed] was called
    // before.
    if (!_enabled)
      return;
    boolean new_has_sel = newSelStart != newSelEnd;
    if (new_has_sel || _has_selection) // Selection was on or is now on.
    {
      _cursor = newSelStart;
      _has_selection = new_has_sel;
      refresh_current_word();
    }
    else if (newSelStart != _cursor)
    {
      _w_cursor += newSelStart - _cursor;
      _cursor = newSelStart;
      if (_w_cursor < -_w.length() || _w_cursor > 0)
        refresh_current_word();
    }
  }

  public void event_sent(int code, int meta)
  {
    if (!_enabled)
      return;
    switch (code)
    {
      case KeyEvent.KEYCODE_DEL:
        int cursor = _w.length() + _w_cursor;
        if (meta == 0 && cursor > 0 && cursor <= _w.length())
          remove_surrounding_text(Character.charCount(Character.codePointBefore(_w, cursor)), 0);
        else
          delayed_refresh();
        break;
      default:
        delayed_refresh();
        break;
    }
  }

  public void remove_surrounding_text(int remove_before, int remove_after)
  {
    if (!_enabled)
      return;
    remove_before = Math.max(0, remove_before);
    remove_after = Math.max(0, remove_after);
    int len = _w.length();
    int c = Math.max(0, Math.min(len + _w_cursor, len));
    int start = Math.max(c - remove_before, 0);
    int end = Math.max(Math.min(c + remove_after, len), 0);
    if (start < end)
    {
      _w.delete(start, end);
    }
    _cursor = Math.max(0, _cursor - remove_before);
    _w_cursor = start - _w.length();
    callback();
  }

  void callback()
  {
    String w = _w.toString();
    _callback.currently_typed_word(w);
  }

  /** Estimate the currently typed word after [chars] has been typed. */
  void type_chars(CharSequence s, int start, int end)
  {
    if (s == null || start < 0 || end < start || end > s.length())
      return;
    int insert_pos = Math.max(0, Math.min(_w.length() + _w_cursor, _w.length()));
    String suffix = _w.substring(insert_pos);
    StringBuilder prefix = new StringBuilder(_w.substring(0, insert_pos));
    // Classify whole code points, but keep Android's UTF-16 cursor offsets.
    for (int i = start; i < end;)
    {
      int c = Character.codePointAt(s, i);
      int size = Character.charCount(c);
      if (i + size > end)
      {
        c = s.charAt(i);
        size = 1;
      }
      if (is_word_char(c))
        prefix.append(s, i, i + size);
      else
      {
        if (prefix.length() > 0)
          _last_completed_word = prefix.toString();
        prefix.setLength(0);
      }
      i += size;
    }
    _cursor += end - start;
    _w.setLength(0);
    _w.append(prefix).append(suffix);
    _w_cursor = -suffix.length();
  }

  void type_chars(CharSequence s)
  {
    if (s != null)
      type_chars(s, 0, s.length());
  }

  /** Append chars to the current word without moving the cursor. Return the
      number of characters that were added in the current word. */
  int append_chars(CharSequence s, int start, int end)
  {
    int i = start;
    while (i < end)
    {
      int c = Character.codePointAt(s, i);
      if (!is_word_char(c))
        break;
      _w.appendCodePoint(c);
      i += Character.charCount(c);
    }
    return i - start;
  }

  int append_chars(CharSequence s)
  {
    return append_chars(s, 0, s.length());
  }

  /** Refresh the current word by immediately querying the editor. */
  void refresh_current_word()
  {
    Logs.debug("Refresh current word");
    _refresh_pending = false;
    _w_cursor = 0;
    if (_ic == null)
    {
      set_current_word("");
      return;
    }
    try
    {
      if (_has_selection)
        set_current_word("");
      else if (VERSION.SDK_INT >= 31)
        set_current_word(_ic.getSurroundingText(120, 120, 0));
      else
        set_current_word(_ic.getTextBeforeCursor(120, 0));
    }
    catch (Throwable t)
    {
      set_current_word("");
    }
  }

  /** Refresh the current word by immediately querying the editor. */
  void set_current_word(CharSequence text_before_cursor)
  {
    _w.setLength(0);
    _w_cursor = 0;
    int saved_cursor = _cursor;
    type_chars(text_before_cursor);
    _cursor = saved_cursor;
    callback();
  }

  /** Like above but take the text after the cursor into account. */
  @TargetApi(31)
  void set_current_word(SurroundingText st)
  {
    _w.setLength(0);
    _w_cursor = 0;
    if (st == null)
    {
      callback();
      return;
    }
    int saved_cursor = _cursor;
    int st_sel = st.getSelectionStart();
    CharSequence st_text = st.getText();
    type_chars(st_text, 0, st_sel);
    _w_cursor = -append_chars(st_text, st_sel, st_text.length());
    _cursor = saved_cursor;
    callback();
  }

  /** Wait some time to let the editor finishes reacting to changes and call
      [refresh_current_word]. */
  void delayed_refresh()
  {
    if (_refresh_pending)
      return;
    _refresh_pending = true;
    _handler.postDelayed(delayed_refresh_run, 50);
  }

  Runnable delayed_refresh_run = new Runnable()
  {
    public void run()
    {
      if (_refresh_pending)
        refresh_current_word();
    }
  };

  /** A word is the longest consecutive sequence for which [is_word_char]
      returns [true]. */
  public static boolean is_word_char(int c)
  {
    int type = Character.getType(c);
    return Character.isLetterOrDigit(c)
        || type == Character.NON_SPACING_MARK
        || type == Character.COMBINING_SPACING_MARK
        || (c == '\'');
  }

  public static interface Callback
  {
    public void currently_typed_word(String word);
  }
}
