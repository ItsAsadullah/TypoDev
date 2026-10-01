package typodev.keyboard;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build.VERSION;
import android.preference.PreferenceManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public final class ClipboardHistoryService
{
  /** Start the service on startup and start listening to clipboard changes. */
  public static void on_startup(Context ctx, ClipboardPasteCallback cb)
  {
    get_service(ctx);
    _paste_callback = cb;
  }

  /** Start the service if it hasn't been started before. Returns [null] if the
      feature is unsupported. */
  public static ClipboardHistoryService get_service(Context ctx)
  {
    if (VERSION.SDK_INT <= 11)
      return null;
    if (_service == null)
      _service = new ClipboardHistoryService(ctx);
    return _service;
  }

  public static void set_history_enabled(boolean e)
  {
    Config.globalConfig().set_clipboard_history_enabled(e);
    if (_service == null)
      return;
    if (e)
      _service.add_current_clip(false);
    else
      _service.clear_history();
  }

  /** Send the given string to the editor. */
  public static void paste(String clip)
  {
    markRecentClipPasted();
    if (_paste_callback != null)
      _paste_callback.paste_from_clipboard_pane(clip);
  }

  /** The maximum size limits the amount of user data stored in memory. */
  public static final int MAX_HISTORY_SIZE = 30;

  private static final String PREF_DISMISSED_CLIPS = "pref_dismissed_clipboard_clips";
  private static final Set<String> _dismissedClips = new HashSet<String>();
  private static boolean _dismissedClipsLoaded = false;

  private static String _lastCopiedText = null;
  private static long _lastCopiedTimestamp = 0L;
  private static boolean _lastCopiedPasted = true;

  private static synchronized void ensureDismissedClipsLoaded(Context ctx)
  {
    if (_dismissedClipsLoaded || ctx == null) return;
    try
    {
      SharedPreferences sp = DirectBootAwarePreferences.get_shared_preferences(ctx);
      Set<String> saved = sp.getStringSet(PREF_DISMISSED_CLIPS, null);
      if (saved != null)
      {
        _dismissedClips.addAll(saved);
      }
      _dismissedClipsLoaded = true;
    }
    catch (Throwable ignored) {}
  }

  public static synchronized boolean isClipDismissed(String text)
  {
    if (text == null || text.trim().isEmpty()) return true;
    if (_service != null && _service._ctx != null)
    {
      ensureDismissedClipsLoaded(_service._ctx);
    }
    return _dismissedClips.contains(text.trim());
  }

  public static synchronized String getRecentUnpastedClip()
  {
    if (_lastCopiedPasted || _lastCopiedText == null || _lastCopiedText.trim().isEmpty())
    {
      return null;
    }
    if (isClipDismissed(_lastCopiedText))
    {
      return null;
    }
    long elapsed = System.currentTimeMillis() - _lastCopiedTimestamp;
    // Suggest unpasted clip within 5 minutes
    if (elapsed < 5 * 60 * 1000L)
    {
      return _lastCopiedText;
    }
    return null;
  }

  public static synchronized void markRecentClipPasted()
  {
    _lastCopiedPasted = true;
    if (_lastCopiedText != null && !_lastCopiedText.trim().isEmpty())
    {
      addDismissedClip(_lastCopiedText.trim());
    }
  }

  public static synchronized void markClipPasted(String text)
  {
    _lastCopiedPasted = true;
    if (text != null && !text.trim().isEmpty())
    {
      addDismissedClip(text.trim());
    }
    else if (_lastCopiedText != null && !_lastCopiedText.trim().isEmpty())
    {
      addDismissedClip(_lastCopiedText.trim());
    }
  }

  public static synchronized void dismissRecentClip()
  {
    _lastCopiedPasted = true;
    if (_lastCopiedText != null && !_lastCopiedText.trim().isEmpty())
    {
      addDismissedClip(_lastCopiedText.trim());
    }
  }

  public static synchronized void dismissClip(String text)
  {
    _lastCopiedPasted = true;
    if (text != null && !text.trim().isEmpty())
    {
      addDismissedClip(text.trim());
    }
    else if (_lastCopiedText != null && !_lastCopiedText.trim().isEmpty())
    {
      addDismissedClip(_lastCopiedText.trim());
    }
  }

  private static synchronized void addDismissedClip(String text)
  {
    if (_service != null && _service._ctx != null)
    {
      ensureDismissedClipsLoaded(_service._ctx);
    }
    _dismissedClips.add(text);
    if (_dismissedClips.size() > 60)
    {
      Iterator<String> it = _dismissedClips.iterator();
      if (it.hasNext())
      {
        it.next();
        it.remove();
      }
    }
    if (_service != null && _service._ctx != null)
    {
      try
      {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(_service._ctx);
        sp.edit().putStringSet(PREF_DISMISSED_CLIPS, new HashSet<String>(_dismissedClips)).apply();
        DirectBootAwarePreferences.copy_preferences_to_protected_storage(_service._ctx, sp);
      }
      catch (Throwable ignored) {}
    }
  }

  public static synchronized void setRecentCopiedClip(String text)
  {
    if (text != null && !text.trim().isEmpty())
    {
      String clean = text.trim();
      _lastCopiedText = clean;
      _lastCopiedTimestamp = System.currentTimeMillis();
      _lastCopiedPasted = false;
      if (_service != null && _service._ctx != null)
      {
        ensureDismissedClipsLoaded(_service._ctx);
      }
      if (_dismissedClips.remove(clean) && _service != null && _service._ctx != null)
      {
        try
        {
          SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(_service._ctx);
          sp.edit().putStringSet(PREF_DISMISSED_CLIPS, new HashSet<String>(_dismissedClips)).apply();
          DirectBootAwarePreferences.copy_preferences_to_protected_storage(_service._ctx, sp);
        }
        catch (Throwable ignored) {}
      }
    }
  }

  static ClipboardHistoryService _service = null;
  static ClipboardPasteCallback _paste_callback = null;

  Context _ctx;
  ClipboardManager _cm;
  List<HistoryEntry> _history;
  OnClipboardHistoryChange _listener = null;

  ClipboardHistoryService(Context ctx)
  {
    _ctx = ctx != null ? ctx.getApplicationContext() : null;
    _history = new ArrayList<HistoryEntry>();
    _cm = (ClipboardManager)ctx.getSystemService(Context.CLIPBOARD_SERVICE);
    try
    {
      _cm.addPrimaryClipChangedListener(this.new SystemListener());
    }
    catch (Throwable ignored) {}
    ensureDismissedClipsLoaded(_ctx);
    add_current_clip(false);
  }

  public synchronized List<String> clear_expired_and_get_history()
  {
    long now_ms = System.currentTimeMillis();
    List<String> dst = new ArrayList<String>();
    Iterator<HistoryEntry> it = _history.iterator();
    while (it.hasNext())
    {
      HistoryEntry ent = it.next();
      if (ent.expiry_timestamp <= now_ms)
        it.remove();
      else if (!ent.isImage)
        dst.add(ent.content);
    }
    return dst;
  }

  public synchronized List<typodev.keyboard.clipboard.ClipboardItem> clear_expired_and_get_history_items()
  {
    long now_ms = System.currentTimeMillis();
    List<typodev.keyboard.clipboard.ClipboardItem> dst = new ArrayList<>();
    Iterator<HistoryEntry> it = _history.iterator();
    while (it.hasNext())
    {
      HistoryEntry ent = it.next();
      if (ent.expiry_timestamp <= now_ms)
      {
        it.remove();
      }
      else
      {
        if (ent.isImage)
        {
          dst.add(new typodev.keyboard.clipboard.ClipboardItem(ent.content, System.currentTimeMillis(), false, true, ent.uriString));
        }
        else
        {
          dst.add(new typodev.keyboard.clipboard.ClipboardItem(ent.content, System.currentTimeMillis(), false));
        }
      }
    }
    return dst;
  }

  /** This will call [on_clipboard_history_change]. */
  public synchronized void remove_history_entry(String clip)
  {
    int last_pos = _history.size() - 1;
    if (clip == null) return;
    boolean last_pos_changed = false;
    for (int pos = last_pos; pos >= 0; pos--)
    {
      HistoryEntry entry = _history.get(pos);
      if (entry == null || !clip.equals(entry.content))
        continue;
      // Removing the current clipboard, clear the system clipboard.
      if (pos == last_pos)
        last_pos_changed = true;
      _history.remove(pos);
    }
    if (last_pos_changed)
    {
      if (VERSION.SDK_INT >= 28)
        _cm.clearPrimaryClip();
      else
        _cm.setText("");
    }
    if (_listener != null)
      _listener.on_clipboard_history_change();
  }

  /** Add clipboard entries to the history, skipping consecutive duplicates and
      empty strings. */
  public synchronized void add_clip(String clip)
  {
    add_clip(clip, true);
  }

  public synchronized void add_clip(String clip, boolean isFreshCopy)
  {
    if (!Config.globalConfig().clipboard_history_enabled)
      return;
    int size = _history.size();
    if (clip.equals("") || (size > 0 && _history.get(0).content.equals(clip)))
    {
      if (isFreshCopy && !isClipDismissed(clip))
      {
        setRecentCopiedClip(clip);
      }
      return;
    }
    if (size >= MAX_HISTORY_SIZE)
      _history.remove(size - 1);
    _history.add(0, new HistoryEntry(clip));
    if (isFreshCopy || !isClipDismissed(clip))
    {
      setRecentCopiedClip(clip);
    }
    if (_listener != null)
      _listener.on_clipboard_history_change();
  }

  public synchronized void add_image_clip(String uriString)
  {
    if (!Config.globalConfig().clipboard_history_enabled || uriString == null || uriString.isEmpty())
      return;
    int size = _history.size();
    if (size > 0 && _history.get(0).content.equals(uriString))
      return;
    if (size >= MAX_HISTORY_SIZE)
      _history.remove(size - 1);
    _history.add(0, new HistoryEntry("[Image]", true, uriString));
    if (_listener != null)
      _listener.on_clipboard_history_change();
  }

  public synchronized void clear_history()
  {
    _history.clear();
    _lastCopiedText = null;
    _lastCopiedPasted = true;
    if (_listener != null)
      _listener.on_clipboard_history_change();
  }

  public synchronized void clear_all_history()
  {
    clear_history();
  }

  public synchronized void remove_history_entries(List<String> list)
  {
    if (list == null || list.isEmpty()) return;
    for (String s : list)
    {
      remove_history_entry(s);
    }
  }

  public void set_on_clipboard_history_change(OnClipboardHistoryChange l) { _listener = l; }

  public static interface OnClipboardHistoryChange
  {
    public void on_clipboard_history_change();
  }

  /** Add what is currently in the system clipboard into the history. */
  public synchronized void add_current_clip()
  {
    add_current_clip(false);
  }

  public synchronized void add_current_clip(boolean isFreshCopy)
  {
    if (_cm == null)
      return;
    ClipData clip = null;
    // getPrimaryClip might throw when the keyboard is disconnected or not in foreground.
    try
    {
      clip = _cm.getPrimaryClip();
    }
    catch (Throwable _e)
    {
      return;
    }
    if (clip == null)
      return;
    int count = clip.getItemCount();
    for (int i = 0; i < count; i++)
    {
      ClipData.Item itm = clip.getItemAt(i);
      if (itm == null)
        continue;
      CharSequence text = itm.getText();
      if ((text == null || text.length() == 0) && _ctx != null)
      {
        try
        {
          text = itm.coerceToText(_ctx);
        }
        catch (Throwable ignored) {}
      }
      if (text != null && text.length() > 0)
      {
        add_clip(text.toString(), isFreshCopy);
      }
      else if (itm.getUri() != null)
      {
        add_image_clip(itm.getUri().toString());
      }
    }
  }

  int get_history_ttl_minutes() {
    return Config.globalConfig().clipboard_history_duration;
  }

  final class SystemListener implements ClipboardManager.OnPrimaryClipChangedListener
  {
    public SystemListener() {}

    @Override
    public void onPrimaryClipChanged()
    {
      add_current_clip(true);
    }
  }

  static final class HistoryEntry
  {
    public final String content;
    public final long expiry_timestamp;
    public final boolean isImage;
    public final String uriString;

    public HistoryEntry(String c)
    {
      this(c, false, null);
    }

    public HistoryEntry(String c, boolean isImage, String uriString)
    {
      this.content = c;
      this.isImage = isImage;
      this.uriString = uriString;

      // Sensitive data auto-expiry: 10 minutes for standalone OTP codes
      boolean isOtp = (c != null) && c.trim().matches("^\\d{4,8}$");
      if (isOtp)
      {
        this.expiry_timestamp = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10);
      }
      else
      {
        final int historyTtlMinutes = (_service != null) ? _service.get_history_ttl_minutes() : 60;
        if (historyTtlMinutes >= 0) {
          this.expiry_timestamp = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(historyTtlMinutes);
        } else {
          this.expiry_timestamp = Long.MAX_VALUE;
        }
      }
    }
  }

  public interface ClipboardPasteCallback
  {
    public void paste_from_clipboard_pane(String content);
  }
}
