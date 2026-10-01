package typodev.keyboard.symbol;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import typodev.keyboard.Logs;

public final class CustomSymbolStore
{
  public static final String PREF_KEY = "custom_key_symbols_map";

  public static class Mapping
  {
    public String baseKey;
    public int pos; // 0 to 8
    public String symbol;

    public Mapping(String baseKey, int pos, String symbol)
    {
      this.baseKey = (baseKey == null) ? "" : baseKey.toLowerCase().trim();
      this.pos = pos;
      this.symbol = (symbol == null) ? "" : symbol.trim();
    }

    public String toSerializedString()
    {
      return baseKey + "|" + pos + "|" + symbol;
    }

    public static Mapping fromSerializedString(String str)
    {
      if (str == null || str.isEmpty()) return null;
      int idx1 = str.indexOf('|');
      if (idx1 <= 0) return null;
      int idx2 = str.indexOf('|', idx1 + 1);
      if (idx2 <= idx1) return null;
      String key = str.substring(0, idx1).trim();
      int pos;
      try
      {
        pos = Integer.parseInt(str.substring(idx1 + 1, idx2).trim());
      }
      catch (Exception e)
      {
        return null;
      }
      String sym = str.substring(idx2 + 1);
      if (key.isEmpty() || sym.isEmpty()) return null;
      return new Mapping(key, pos, sym);
    }

    public JSONObject toJson()
    {
      try
      {
        JSONObject obj = new JSONObject();
        obj.put("baseKey", baseKey);
        obj.put("pos", pos);
        obj.put("symbol", symbol);
        return obj;
      }
      catch (Throwable t)
      {
        return null;
      }
    }

    public static Mapping fromJson(JSONObject obj)
    {
      if (obj == null) return null;
      String baseKey = obj.optString("baseKey", "");
      int pos = obj.optInt("pos", 1);
      String symbol = obj.optString("symbol", "");
      if (baseKey.isEmpty() || symbol.isEmpty()) return null;
      return new Mapping(baseKey, pos, symbol);
    }

    @Override
    public boolean equals(Object o)
    {
      if (this == o) return true;
      if (!(o instanceof Mapping)) return false;
      Mapping m = (Mapping) o;
      return pos == m.pos && baseKey.equalsIgnoreCase(m.baseKey);
    }

    @Override
    public int hashCode()
    {
      return 31 * baseKey.toLowerCase().hashCode() + pos;
    }
  }

  public static String serializeList(List<Mapping> list)
  {
    if (list == null || list.isEmpty()) return "";
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < list.size(); i++)
    {
      Mapping m = list.get(i);
      if (m != null && m.baseKey != null && !m.baseKey.isEmpty() && m.symbol != null && !m.symbol.isEmpty())
      {
        if (sb.length() > 0) sb.append("\n");
        sb.append(m.toSerializedString());
      }
    }
    return sb.toString();
  }

  public static List<Mapping> deserializeList(String data)
  {
    List<Mapping> list = new ArrayList<>();
    if (data == null || data.trim().isEmpty()) return list;

    // Check if JSON array (starts with '[')
    if (data.trim().startsWith("["))
    {
      try
      {
        JSONArray arr = new JSONArray(data);
        for (int i = 0; i < arr.length(); i++)
        {
          JSONObject obj = arr.optJSONObject(i);
          Mapping m = Mapping.fromJson(obj);
          if (m != null) list.add(m);
        }
        if (!list.isEmpty()) return list;
      }
      catch (Throwable ignored) {}
    }

    // Line delimited
    String[] lines = data.split("\n");
    for (String line : lines)
    {
      Mapping m = Mapping.fromSerializedString(line.trim());
      if (m != null) list.add(m);
    }
    return list;
  }

  public static List<Mapping> getMappings(SharedPreferences prefs)
  {
    if (prefs == null) return new ArrayList<>();
    String raw = prefs.getString(PREF_KEY, null);
    return deserializeList(raw);
  }

  public static void saveMappings(SharedPreferences prefs, List<Mapping> mappings)
  {
    if (prefs == null) return;
    try
    {
      String serialized = serializeList(mappings);
      prefs.edit().putString(PREF_KEY, serialized).apply();
    }
    catch (Exception e)
    {
      Logs.exn("CustomSymbolStore.saveMappings", e);
    }
  }

  public static void addOrUpdateMapping(SharedPreferences prefs, Mapping newMapping)
  {
    if (prefs == null || newMapping == null) return;
    List<Mapping> list = getMappings(prefs);
    for (int i = 0; i < list.size(); i++)
    {
      Mapping existing = list.get(i);
      if (existing != null && existing.baseKey != null && newMapping.baseKey != null
          && existing.baseKey.equalsIgnoreCase(newMapping.baseKey) && existing.pos == newMapping.pos)
      {
        list.set(i, newMapping);
        saveMappings(prefs, list);
        return;
      }
    }
    list.add(newMapping);
    saveMappings(prefs, list);
  }

  public static void removeMapping(SharedPreferences prefs, String baseKey, int pos)
  {
    if (prefs == null || baseKey == null) return;
    List<Mapping> list = getMappings(prefs);
    boolean removed = false;
    for (int i = list.size() - 1; i >= 0; i--)
    {
      Mapping existing = list.get(i);
      if (existing != null && existing.baseKey != null
          && existing.baseKey.equalsIgnoreCase(baseKey) && existing.pos == pos)
      {
        list.remove(i);
        removed = true;
      }
    }
    if (removed)
    {
      saveMappings(prefs, list);
    }
  }

  public static void clearAll(SharedPreferences prefs)
  {
    if (prefs == null) return;
    prefs.edit().remove(PREF_KEY).apply();
  }

  public static final String PREF_RESERVED_KEYS = "custom_reserved_keys";

  public static String serializeReservedKeys(List<String> reservedKeys)
  {
    if (reservedKeys == null || reservedKeys.isEmpty()) return "";
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < reservedKeys.size(); i++)
    {
      String k = reservedKeys.get(i);
      if (k != null && !k.trim().isEmpty())
      {
        if (sb.length() > 0) sb.append("\n");
        sb.append(k.trim());
      }
    }
    return sb.toString();
  }

  public static List<String> deserializeReservedKeys(String raw)
  {
    List<String> list = new ArrayList<>();
    if (raw == null || raw.trim().isEmpty()) return list;
    String[] lines = raw.split("\n");
    for (String l : lines)
    {
      String trimmed = l.trim();
      if (!trimmed.isEmpty())
      {
        list.add(trimmed);
      }
    }
    return list;
  }

  public static List<String> getReservedKeys(SharedPreferences prefs)
  {
    if (prefs == null) return new ArrayList<>();
    String raw = prefs.getString(PREF_RESERVED_KEYS, null);
    return deserializeReservedKeys(raw);
  }

  public static void saveReservedKeys(SharedPreferences prefs, List<String> reservedKeys)
  {
    if (prefs == null) return;
    if (reservedKeys == null || reservedKeys.isEmpty())
    {
      prefs.edit().remove(PREF_RESERVED_KEYS).apply();
      return;
    }
    prefs.edit().putString(PREF_RESERVED_KEYS, serializeReservedKeys(reservedKeys)).apply();
  }

  public static class SpecialAction
  {
    public final String code;
    public final String label;
    public final String icon;

    public SpecialAction(String code, String label, String icon)
    {
      this.code = code;
      this.label = label;
      this.icon = icon;
    }
  }

  public static final SpecialAction[] SPECIAL_ACTIONS = new SpecialAction[]{
      new SpecialAction("selectAll", "Select All", "▦"),
      new SpecialAction("copy", "Copy", "⎘"),
      new SpecialAction("paste", "Paste", "📋"),
      new SpecialAction("cut", "Cut", "✂"),
      new SpecialAction("undo", "Undo", "↶"),
      new SpecialAction("redo", "Redo", "↷"),
      new SpecialAction("shareText", "Share Text", "↗"),
      new SpecialAction("pasteAsPlainText", "Paste Plain", "📄"),
      new SpecialAction("delete_word", "Delete Word", "⌫W"),
      new SpecialAction("forward_delete_word", "Fwd Del Word", "W⌦"),
      new SpecialAction("up", "Arrow Up", "↑"),
      new SpecialAction("down", "Arrow Down", "↓"),
      new SpecialAction("left", "Arrow Left", "←"),
      new SpecialAction("right", "Arrow Right", "→"),
      new SpecialAction("ctrl", "Ctrl", "Ctrl"),
      new SpecialAction("fn", "Fn", "Fn"),
      new SpecialAction("shift", "Shift", "⇧"),
      new SpecialAction("alt", "Alt", "Alt"),
      new SpecialAction("meta", "Meta", "Meta"),
      new SpecialAction("esc", "Esc", "Esc"),
      new SpecialAction("tab", "Tab", "⇥"),
      new SpecialAction("home", "Home", "↖"),
      new SpecialAction("end", "End", "↘"),
      new SpecialAction("page_up", "Page Up", "⇞"),
      new SpecialAction("page_down", "Page Down", "⇟"),
      new SpecialAction("menu", "Menu", "Menu"),
      new SpecialAction("scroll_lock", "Scroll Lock", "Scrl"),
      new SpecialAction("superscript", "Superscript", "x²"),
      new SpecialAction("subscript", "Subscript", "x₂"),
      new SpecialAction("switch_clipboard", "Clipboard", "📋"),
      new SpecialAction("voice_typing", "Voice Typing", "🎙"),
      new SpecialAction("change_method", "Switch Keyboard", "⌨"),
      new SpecialAction("capslock", "Caps Lock", "⇪"),
      new SpecialAction("compose", "Compose", "◆"),
      new SpecialAction("switch_greekmath", "Greek & Math", "πλ∇¬"),
      new SpecialAction("f11_placeholder", "F11", "F11"),
      new SpecialAction("f12_placeholder", "F12", "F12"),
      new SpecialAction("zwj", "Zero-Width Joiner", "ZWJ"),
      new SpecialAction("zwnj", "Zero-Width Non-Joiner", "ZWNJ"),
      new SpecialAction("nbsp", "Non-Breaking Space", "NBSP"),
      new SpecialAction("nnbsp", "Narrow NBSP", "NNBSP"),
      // Currency & extra symbols in Add Keys
      new SpecialAction("€", "Euro (€)", "€"),
      new SpecialAction("£", "Pound (£)", "£"),
      new SpecialAction("§", "Section (§)", "§"),
      new SpecialAction("ß", "Eszett (ß)", "ß"),
      new SpecialAction("†", "Dagger (†)", "†"),
      new SpecialAction("ª", "Feminine Ordinal (ª)", "ª"),
      new SpecialAction("º", "Masculine Ordinal (º)", "º"),
      // Dead keys / accents in Add Keys
      new SpecialAction("accent_aigu", "Acute Accent (´)", "´"),
      new SpecialAction("accent_grave", "Grave Accent (`)", "`"),
      new SpecialAction("accent_circonflexe", "Circumflex (^)", "^"),
      new SpecialAction("accent_tilde", "Tilde (~)", "~"),
      new SpecialAction("accent_trema", "Diaeresis / Umlaut (¨)", "¨"),
      new SpecialAction("accent_caron", "Caron (ˇ)", "ˇ"),
      new SpecialAction("accent_cedille", "Cedilla (¸)", "¸"),
      new SpecialAction("accent_macron", "Macron (¯)", "¯"),
      new SpecialAction("accent_ring", "Ring Above (˚)", "˚"),
      new SpecialAction("accent_ogonek", "Ogonek (˛)", "˛"),
      new SpecialAction("accent_breve", "Breve (˘)", "˘"),
      new SpecialAction("accent_dot_above", "Dot Above (˙)", "˙"),
      new SpecialAction("accent_double_aigu", "Double Acute (˝)", "˝"),
      new SpecialAction("accent_slash", "Stroke / Slash (̷)", "̷"),
      new SpecialAction("accent_bar", "Bar (̵)", "̵"),
      new SpecialAction("accent_dot_below", "Dot Below (̣)", "̣"),
      new SpecialAction("accent_hook_above", "Hook Above (̉)", "̉"),
      new SpecialAction("accent_horn", "Horn (̛)", "̛"),
      new SpecialAction("accent_double_grave", "Double Grave (̏)", "̏"),
      new SpecialAction("accent_small_caps", "Small Caps (ᴀ)", "ᴀ")
  };

  public static final String SYMBOL_EMPTY = "__EMPTY__";

  public static boolean isSpecialAction(String sym)
  {
    if (sym == null || sym.isEmpty()) return false;
    if (SYMBOL_EMPTY.equalsIgnoreCase(sym) || "none".equalsIgnoreCase(sym)) return true;
    for (SpecialAction sa : SPECIAL_ACTIONS)
    {
      if (sa.code.equalsIgnoreCase(sym))
        return true;
    }
    return false;
  }

  public static String getFriendlyActionLabel(String sym)
  {
    if (sym == null || sym.isEmpty()) return "";
    if (SYMBOL_EMPTY.equalsIgnoreCase(sym) || "none".equalsIgnoreCase(sym))
      return "Empty (Cleared)";
    for (SpecialAction sa : SPECIAL_ACTIONS)
    {
      if (sa.code.equalsIgnoreCase(sym))
        return sa.label + " (" + sa.icon + ")";
    }
    return sym;
  }

  public static String getCanonicalCodeForKeyValue(typodev.keyboard.KeyValue kv)
  {
    if (kv == null) return null;
    switch (kv.getKind())
    {
      case Editing:
        switch (kv.getEditing())
        {
          case SELECT_ALL: return "selectAll";
          case COPY: return "copy";
          case PASTE: return "paste";
          case CUT: return "cut";
          case UNDO: return "undo";
          case REDO: return "redo";
          case DELETE_WORD: return "delete_word";
          case FORWARD_DELETE_WORD: return "forward_delete_word";
          case PASTE_PLAIN: return "pasteAsPlainText";
          case SHARE: return "shareText";
          case SPACE_BAR: return "space";
          case BACKSPACE: return "backspace";
          default: return kv.getString();
        }
      case Keyevent:
        switch (kv.getKeyevent())
        {
          case android.view.KeyEvent.KEYCODE_DPAD_UP: return "up";
          case android.view.KeyEvent.KEYCODE_DPAD_DOWN: return "down";
          case android.view.KeyEvent.KEYCODE_DPAD_LEFT: return "left";
          case android.view.KeyEvent.KEYCODE_DPAD_RIGHT: return "right";
          case android.view.KeyEvent.KEYCODE_ESCAPE: return "esc";
          case android.view.KeyEvent.KEYCODE_TAB: return "tab";
          case android.view.KeyEvent.KEYCODE_MOVE_HOME: return "home";
          case android.view.KeyEvent.KEYCODE_MOVE_END: return "end";
          case android.view.KeyEvent.KEYCODE_PAGE_UP: return "page_up";
          case android.view.KeyEvent.KEYCODE_PAGE_DOWN: return "page_down";
          case android.view.KeyEvent.KEYCODE_ENTER: return "enter";
          case android.view.KeyEvent.KEYCODE_FORWARD_DEL: return "delete";
          case android.view.KeyEvent.KEYCODE_MENU: return "menu";
          case android.view.KeyEvent.KEYCODE_SCROLL_LOCK: return "scroll_lock";
          case android.view.KeyEvent.KEYCODE_INSERT: return "insert";
          case android.view.KeyEvent.KEYCODE_F1: return "f1";
          case android.view.KeyEvent.KEYCODE_F2: return "f2";
          case android.view.KeyEvent.KEYCODE_F3: return "f3";
          case android.view.KeyEvent.KEYCODE_F4: return "f4";
          case android.view.KeyEvent.KEYCODE_F5: return "f5";
          case android.view.KeyEvent.KEYCODE_F6: return "f6";
          case android.view.KeyEvent.KEYCODE_F7: return "f7";
          case android.view.KeyEvent.KEYCODE_F8: return "f8";
          case android.view.KeyEvent.KEYCODE_F9: return "f9";
          case android.view.KeyEvent.KEYCODE_F10: return "f10";
          case android.view.KeyEvent.KEYCODE_F11: return "f11_placeholder";
          case android.view.KeyEvent.KEYCODE_F12: return "f12_placeholder";
          default: return kv.getString();
        }
      case Modifier:
        switch (kv.getModifier())
        {
          case SHIFT: return "shift";
          case CTRL: return "ctrl";
          case FN: return "fn";
          case ALT: return "alt";
          case META: return "meta";
          case SUPERSCRIPT: return "superscript";
          case SUBSCRIPT: return "subscript";
          case ORDINAL: return "ordinal";
          case ARROWS: return "arrows";
          case BOX: return "box";
          case AIGU: return "accent_aigu";
          case CARON: return "accent_caron";
          case CEDILLE: return "accent_cedille";
          case CIRCONFLEXE: return "accent_circonflexe";
          case GRAVE: return "accent_grave";
          case MACRON: return "accent_macron";
          case RING: return "accent_ring";
          case TILDE: return "accent_tilde";
          case TREMA: return "accent_trema";
          case OGONEK: return "accent_ogonek";
          case DOT_ABOVE: return "accent_dot_above";
          case DOUBLE_AIGU: return "accent_double_aigu";
          case SLASH: return "accent_slash";
          case ARROW_RIGHT: return "accent_arrow_right";
          case BREVE: return "accent_breve";
          case BAR: return "accent_bar";
          case DOT_BELOW: return "accent_dot_below";
          case HORN: return "accent_horn";
          case HOOK_ABOVE: return "accent_hook_above";
          case DOUBLE_GRAVE: return "accent_double_grave";
          case SMALL_CAPS: return "accent_small_caps";
          default: return kv.getString();
        }
      case Event:
        switch (kv.getEvent())
        {
          case SWITCH_CLIPBOARD: return "switch_clipboard";
          case SWITCH_VOICE_TYPING: return "voice_typing";
          case CHANGE_METHOD_PICKER: return "change_method";
          case CHANGE_METHOD_PREV: return "change_method_prev";
          case CHANGE_METHOD_NEXT: return "change_method_next";
          case CAPS_LOCK: return "capslock";
          case SWITCH_GREEKMATH: return "switch_greekmath";
          case CONFIG: return "config";
          case SWITCH_NUMERIC: return "switch_numeric";
          case SWITCH_EMOJI: return "switch_emoji";
          case SWITCH_FORWARD: return "switch_forward";
          case SWITCH_BACKWARD: return "switch_backward";
          default: return kv.getString();
        }
      case Placeholder:
        switch (kv.getPlaceholder())
        {
          case F11: return "f11_placeholder";
          case F12: return "f12_placeholder";
          case COMPOSE_CANCEL: return "compose_cancel";
          case REMOVED: return null;
          default: return kv.getString();
        }
      case Compose_pending:
        return "compose";
      case Char:
        char ch = kv.getChar();
        if (ch == '\u200D') return "zwj";
        if (ch == '\u200C') return "zwnj";
        if (ch == '\u00A0') return "nbsp";
        if (ch == '\u202F') return "nnbsp";
        if (ch == '\t') return "\\t";
        if (ch == '\n') return "\\n";
        return String.valueOf(ch);
      default:
        String s = kv.getString();
        if (s != null && !s.isEmpty())
        {
          char c0 = s.charAt(0);
          if (c0 >= 0xE000 && c0 <= 0xF8FF)
          {
            return null;
          }
          return s;
        }
        return null;
    }
  }

  public static String formatKeyShortBadge(typodev.keyboard.KeyValue kv)
  {
    if (kv == null) return null;
    switch (kv.getKind())
    {
      case Editing:
        switch (kv.getEditing())
        {
          case SELECT_ALL: return "▦";
          case COPY: return "⎘";
          case PASTE: return "📋";
          case CUT: return "✂";
          case UNDO: return "↶";
          case REDO: return "↷";
          case DELETE_WORD: return "⌫W";
          case FORWARD_DELETE_WORD: return "W⌦";
          case PASTE_PLAIN: return "📄";
          case SHARE: return "↗";
          case SPACE_BAR: return "␣";
          case BACKSPACE: return "⌫";
          default: return kv.getString();
        }
      case Keyevent:
        switch (kv.getKeyevent())
        {
          case android.view.KeyEvent.KEYCODE_DPAD_UP: return "↑";
          case android.view.KeyEvent.KEYCODE_DPAD_DOWN: return "↓";
          case android.view.KeyEvent.KEYCODE_DPAD_LEFT: return "←";
          case android.view.KeyEvent.KEYCODE_DPAD_RIGHT: return "→";
          case android.view.KeyEvent.KEYCODE_ESCAPE: return "Esc";
          case android.view.KeyEvent.KEYCODE_TAB: return "⇥";
          case android.view.KeyEvent.KEYCODE_MOVE_HOME: return "↖";
          case android.view.KeyEvent.KEYCODE_MOVE_END: return "↘";
          case android.view.KeyEvent.KEYCODE_PAGE_UP: return "⇞";
          case android.view.KeyEvent.KEYCODE_PAGE_DOWN: return "⇟";
          case android.view.KeyEvent.KEYCODE_ENTER: return "↵";
          case android.view.KeyEvent.KEYCODE_FORWARD_DEL: return "⌦";
          case android.view.KeyEvent.KEYCODE_MENU: return "Menu";
          case android.view.KeyEvent.KEYCODE_SCROLL_LOCK: return "Scrl";
          default: return kv.getString();
        }
      case Modifier:
        switch (kv.getModifier())
        {
          case SHIFT: return "⇧";
          case CTRL: return "Ctrl";
          case FN: return "Fn";
          case ALT: return "Alt";
          case META: return "Meta";
          case SUPERSCRIPT: return "x²";
          case SUBSCRIPT: return "x₂";
          default: return "";
        }
      case Event:
        switch (kv.getEvent())
        {
          case SWITCH_CLIPBOARD: return "📋";
          case SWITCH_VOICE_TYPING: return "🎙";
          case CHANGE_METHOD_PICKER:
          case CHANGE_METHOD_PREV:
          case CHANGE_METHOD_NEXT: return "⌨";
          case CAPS_LOCK: return "⇪";
          case SWITCH_GREEKMATH: return "πλ";
          default: return kv.getString();
        }
      case Placeholder:
        switch (kv.getPlaceholder())
        {
          case F11: return "F11";
          case F12: return "F12";
          default: return kv.getString();
        }
      case Compose_pending:
        return "◆";
      case Char:
        char ch = kv.getChar();
        if (ch == '\u200D') return "ZWJ";
        if (ch == '\u200C') return "ZWNJ";
        if (ch == '\u00A0') return "NBSP";
        if (ch == '\u202F') return "NNBSP";
        return String.valueOf(ch);
      default:
        String str = kv.getString();
        if (str != null && !str.trim().isEmpty())
        {
          char c = str.charAt(0);
          if (c >= 0xE000 && c <= 0xF8FF)
          {
            return "";
          }
          return str.trim();
        }
        return null;
    }
  }

  public static String formatKeyValueDisplay(typodev.keyboard.KeyValue kv)
  {
    if (kv == null) return "—";
    switch (kv.getKind())
    {
      case Editing:
        switch (kv.getEditing())
        {
          case SELECT_ALL: return "Select All (▦)";
          case COPY: return "Copy (⎘)";
          case PASTE: return "Paste (📋)";
          case CUT: return "Cut (✂)";
          case UNDO: return "Undo (↶)";
          case REDO: return "Redo (↷)";
          case DELETE_WORD: return "Del Word (⌫)";
          case FORWARD_DELETE_WORD: return "Fwd Del (⌦)";
          case PASTE_PLAIN: return "Paste Plain (📄)";
          case SHARE: return "Share (↗)";
          case SPACE_BAR: return "Space (␣)";
          case BACKSPACE: return "Backspace (⌫)";
          default: return kv.getString();
        }
      case Keyevent:
        switch (kv.getKeyevent())
        {
          case android.view.KeyEvent.KEYCODE_ESCAPE: return "Esc";
          case android.view.KeyEvent.KEYCODE_TAB: return "Tab (⇥)";
          case android.view.KeyEvent.KEYCODE_MOVE_HOME: return "Home (↖)";
          case android.view.KeyEvent.KEYCODE_MOVE_END: return "End (↘)";
          case android.view.KeyEvent.KEYCODE_PAGE_UP: return "PgUp (⇞)";
          case android.view.KeyEvent.KEYCODE_PAGE_DOWN: return "PgDn (⇟)";
          case android.view.KeyEvent.KEYCODE_DPAD_UP: return "↑ (Up)";
          case android.view.KeyEvent.KEYCODE_DPAD_DOWN: return "↓ (Down)";
          case android.view.KeyEvent.KEYCODE_DPAD_LEFT: return "← (Left)";
          case android.view.KeyEvent.KEYCODE_DPAD_RIGHT: return "→ (Right)";
          case android.view.KeyEvent.KEYCODE_ENTER: return "↵ (Enter)";
          case android.view.KeyEvent.KEYCODE_FORWARD_DEL: return "Del (⌦)";
          case android.view.KeyEvent.KEYCODE_MENU: return "Menu";
          case android.view.KeyEvent.KEYCODE_SCROLL_LOCK: return "Scroll Lock";
          default: return kv.getString();
        }
      case Event:
        switch (kv.getEvent())
        {
          case SWITCH_CLIPBOARD: return "Clipboard (📋)";
          case SWITCH_VOICE_TYPING: return "Voice (🎙)";
          case CHANGE_METHOD_PICKER:
          case CHANGE_METHOD_PREV:
          case CHANGE_METHOD_NEXT: return "Switch IME (⌨)";
          case CAPS_LOCK: return "Caps Lock (⇪)";
          case SWITCH_GREEKMATH: return "πλ∇¬";
          case CONFIG: return "Settings (⚙)";
          case SWITCH_NUMERIC: return "123+";
          case SWITCH_EMOJI: return "Emoji (😀)";
          case SWITCH_FORWARD: return "Next Layout (↷)";
          case SWITCH_BACKWARD: return "Prev Layout (↶)";
          case ACTION: return "Action";
          default: return kv.getString();
        }
      case Modifier:
        switch (kv.getModifier())
        {
          case SHIFT: return "Shift (⇧)";
          case CTRL: return "Ctrl";
          case FN: return "Fn";
          case ALT: return "Alt";
          case META: return "Meta";
          case SUPERSCRIPT: return "Superscript (x²)";
          case SUBSCRIPT: return "Subscript (x₂)";
          default: return kv.getString();
        }
      case Placeholder:
        switch (kv.getPlaceholder())
        {
          case F11: return "F11";
          case F12: return "F12";
          default: return kv.getString();
        }
      case Slider:
        switch (kv.getSlider())
        {
          case Cursor_left: return "Cursor Left (⇦)";
          case Cursor_right: return "Cursor Right (⇨)";
          case Cursor_up: return "Cursor Up (⇧)";
          case Cursor_down: return "Cursor Down (⇩)";
          default: return kv.getString();
        }
      case Compose_pending:
        return "Compose (◆)";
      case Char:
        char ch = kv.getChar();
        if (ch == '\u200D') return "Zero-Width Joiner (ZWJ)";
        if (ch == '\u200C') return "Zero-Width Non-Joiner (ZWNJ)";
        if (ch == '\u00A0') return "Non-Breaking Space (NBSP)";
        if (ch == '\u202F') return "Narrow NBSP (NNBSP)";
        return String.valueOf(ch);
      default:
        String str = kv.getString();
        return (str != null && !str.isEmpty()) ? str : "—";
    }
  }

  public static String getPosName(int pos)
  {
    switch (pos)
    {
      case 1: return "Top-Left";
      case 2: return "Top-Right";
      case 3: return "Bottom-Left";
      case 4: return "Bottom-Right";
      case 5: return "Left";
      case 6: return "Right";
      case 7: return "Top";
      case 8: return "Bottom";
      case 0: return "Center";
      default: return "Position " + pos;
    }
  }

  public static String getPosIcon(int pos)
  {
    switch (pos)
    {
      case 1: return "↖";
      case 2: return "↗";
      case 3: return "↙";
      case 4: return "↘";
      case 5: return "←";
      case 6: return "→";
      case 7: return "↑";
      case 8: return "↓";
      case 0: return "•";
      default: return "?";
    }
  }
}
