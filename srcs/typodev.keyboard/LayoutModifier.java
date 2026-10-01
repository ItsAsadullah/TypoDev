package typodev.keyboard;

import android.content.res.Resources;
import android.view.KeyEvent;
import java.util.TreeMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class LayoutModifier
{
  static Config globalConfig;
  static KeyboardData.Row bottom_row;
  static KeyboardData.Row number_row_no_symbols;
  static KeyboardData.Row number_row_symbols;
  static KeyboardData num_pad;
  // Not used in this file but defined here for convenience.
  public static KeyboardData.Row split_middle_column;

  /** Update the layout according to the configuration.
   *  - Remove the switching key if it isn't needed
   *  - Remove "localized" keys from other locales (not in 'extra_keys')
   *  - Replace the action key to show the right label
   *  - Swap the enter and action keys
   *  - Add the optional numpad and number row
   *  - Add the extra keys
   */
  public static KeyboardData modify_layout(KeyboardData kw)
  {
    return modify_layout_internal(kw, true, false);
  }

  /** Modify layout specifically for the customizer preview:
   *  - Includes all extra keys (from settings and device locales)
   *  - Includes bottom row and number row
   *  - Retains standard portrait aspect (no split layout)
   *  - Does NOT bake in custom symbols yet (so base layout defaults/extra keys can be inspected)
   */
  public static KeyboardData modify_layout_for_preview(KeyboardData kw)
  {
    return modify_layout_internal(kw, true, true);
  }

  private static KeyboardData modify_layout_internal(KeyboardData kw, boolean applyCustomSymbols, final boolean isPreview)
  {
    if (kw == null) return null;
    Config config = globalConfig;
    if (config == null)
    {
      config = Config.globalConfig();
    }

    final TreeMap<KeyValue, KeyboardData.PreferredPos> extra_keys = new TreeMap<KeyValue, KeyboardData.PreferredPos>();
    final Set<KeyValue> remove_keys = new HashSet<KeyValue>();
    // Make sure the config key is accessible to avoid being locked in a custom layout
    extra_keys.put(KeyValue.CONFIG, KeyboardData.PreferredPos.ANYWHERE);
    if (config != null)
    {
      if (config.extra_keys_param != null)
        extra_keys.putAll(config.extra_keys_param);
      if (config.extra_keys_custom != null)
        extra_keys.putAll(config.extra_keys_custom);
    }

    // Number row and numpads are added after the modification pass to allow
    // removing the number keys from the main layout.
    KeyboardData.Row added_number_row = null;
    KeyboardData added_numpad = null;
    if (!isPreview && config != null && config.show_numpad && num_pad != null)
    {
      added_numpad = modify_numpad(num_pad, kw);
      remove_keys.addAll(added_numpad.getKeys().keySet());
    }
    else if (config != null && config.add_number_row && !kw.embedded_number_row)
    {
      added_number_row = modify_number_row(config.number_row_symbols ? number_row_symbols : number_row_no_symbols, kw);
      if (!isPreview && config.split_layout)
        added_number_row = LayoutLandscapeModifier.transform_number_row(added_number_row);
      if (added_number_row != null)
        remove_keys.addAll(added_number_row.getKeys(0).keySet());
    }

    // Add the bottom row before computing the extra keys
    if (kw.bottom_row && bottom_row != null)
    {
      boolean hasBottom = false;
      for (KeyboardData.Row r : kw.rows)
      {
        if (r.keys != null)
        {
          for (KeyboardData.Key k : r.keys)
          {
            if (k.role == KeyboardData.Key.Role.Space_bar) { hasBottom = true; break; }
          }
        }
        if (hasBottom) break;
      }
      if (!hasBottom)
      {
        kw = kw.insert_row(bottom_row, kw.rows.size());
      }
    }

    // Split the layout in landscape orientation (not during preview)
    if (!isPreview && config != null && config.split_layout)
      kw = LayoutLandscapeModifier.transform_to_landscape(kw);

    // Compose keys to add to the layout
    Set<KeyValue> extra_keys_keyset = extra_keys.keySet();
    Set<KeyValue> kw_keys = kw.getKeys().keySet();
    if (config != null && config.extra_keys_subtype != null && kw.locale_extra_keys)
    {
      Set<KeyValue> present = new HashSet<KeyValue>(kw_keys);
      present.addAll(extra_keys_keyset);
      config.extra_keys_subtype.compute(extra_keys,
          new ExtraKeys.Query(kw.script, present));
    }
    kw = kw.mapKeys(new KeyboardData.MapKeyValues() {
      public KeyValue apply(KeyValue key, boolean localized)
      {
        if (localized && !extra_keys.containsKey(key))
        {
          if (isPreview)
            return modify_key(key, isPreview);
          return null;
        }
        if (remove_keys.contains(key))
          return null;
        return modify_key(key, isPreview);
      }
    });
    if (added_numpad != null)
      kw = kw.addNumPad(added_numpad);

    // Add extra keys that are not on the layout (including 'loc' keys)
    extra_keys_keyset.removeAll(kw_keys);
    if (extra_keys.size() > 0)
      kw = kw.addExtraKeys(extra_keys.entrySet().iterator());

    // Avoid adding extra keys to the number row
    if (added_number_row != null)
      kw = kw.insert_row(added_number_row, 0);

    if (applyCustomSymbols && config != null && config.custom_symbol_mappings != null && !config.custom_symbol_mappings.isEmpty())
      kw = apply_custom_symbols(kw, config.custom_symbol_mappings);

    return kw;
  }

  public static KeyboardData apply_custom_symbols(KeyboardData kw, java.util.List<typodev.keyboard.symbol.CustomSymbolStore.Mapping> mappings)
  {
    if (kw == null || mappings == null || mappings.isEmpty())
      return kw;

    final java.util.Map<String, java.util.Map<Integer, String>> customMap = new java.util.HashMap<String, java.util.Map<Integer, String>>();
    for (typodev.keyboard.symbol.CustomSymbolStore.Mapping m : mappings)
    {
      if (m == null || m.baseKey == null || m.baseKey.isEmpty() || m.symbol == null || m.symbol.isEmpty())
        continue;
      String k = m.baseKey.toLowerCase().trim();
      if (!customMap.containsKey(k))
        customMap.put(k, new java.util.HashMap<Integer, String>());
      customMap.get(k).put(m.pos, m.symbol);
    }

    if (customMap.isEmpty())
      return kw;

    return kw.mapKeys(new KeyboardData.MapKey() {
      @Override
      public KeyboardData.Key apply(KeyboardData.Key key)
      {
        if (key == null) return null;
        String baseStr = null;
        if (key.role == KeyboardData.Key.Role.Space_bar)
        {
          baseStr = "space";
        }
        else if (key.keys != null && key.keys.length > 0 && key.keys[0] != null)
        {
          KeyValue kv = key.keys[0];
          if (kv.getKind() == KeyValue.Kind.Modifier)
          {
            switch (kv.getModifier())
            {
              case SHIFT: baseStr = "shift"; break;
              case CTRL: baseStr = "ctrl"; break;
              case FN: baseStr = "fn"; break;
              case ALT: baseStr = "alt"; break;
              case META: baseStr = "meta"; break;
              default: break;
            }
          }
          else if (kv.getKind() == KeyValue.Kind.Compose_pending)
          {
            baseStr = "compose";
          }
          else if (kv.getKind() == KeyValue.Kind.Editing)
          {
            switch (kv.getEditing())
            {
              case SPACE_BAR: baseStr = "space"; break;
              case BACKSPACE: baseStr = "backspace"; break;
              default: break;
            }
          }
          else if (kv.getKind() == KeyValue.Kind.Keyevent)
          {
            switch (kv.getKeyevent())
            {
              case KeyEvent.KEYCODE_ENTER: baseStr = "enter"; break;
              case KeyEvent.KEYCODE_DEL: baseStr = "backspace"; break;
              case KeyEvent.KEYCODE_ESCAPE: baseStr = "esc"; break;
              case KeyEvent.KEYCODE_TAB: baseStr = "tab"; break;
              default: break;
            }
          }
          else if (kv.getKind() == KeyValue.Kind.Event && kv.getEvent() == KeyValue.Event.ACTION)
          {
            baseStr = "enter";
          }
          if (baseStr == null)
          {
            baseStr = kv.getString();
          }
          if (baseStr != null) baseStr = baseStr.toLowerCase().trim();
        }

        if (baseStr == null || !customMap.containsKey(baseStr))
          return key;

        java.util.Map<Integer, String> posMap = customMap.get(baseStr);
        if (posMap == null)
          return key;
        KeyboardData.Key currentKey = key;
        for (java.util.Map.Entry<Integer, String> entry : posMap.entrySet())
        {
          int pos = entry.getKey();
          String sym = entry.getValue();
          if (pos >= 0 && pos < 9)
          {
            if (sym == null || sym.isEmpty() || "__EMPTY__".equalsIgnoreCase(sym) || "none".equalsIgnoreCase(sym))
            {
              currentKey = currentKey.withKeyValue(pos, null);
            }
            else
            {
              KeyValue customKv;
              KeyValue special = KeyValue.getSpecialKeyByName(sym);
              if (special != null)
                customKv = special;
              else if (sym.length() == 1)
                customKv = KeyValue.makeCharKey(sym.charAt(0));
              else
                customKv = KeyValue.getKeyByName(sym);

              currentKey = currentKey.withKeyValue(pos, customKv);
            }
          }
        }
        return currentKey;
      }
    });
  }

  /** Handle the numpad layout. The [main_kw] is used to adapt the numpad to
      the main layout's script. */
  public static KeyboardData modify_numpad(KeyboardData kw, KeyboardData main_kw)
  {
    final int map_digit = KeyModifier.modify_numpad_script(main_kw.numpad_script);
    return kw.mapKeys(new KeyboardData.MapKeyValues() {
      public KeyValue apply(KeyValue key, boolean localized)
      {
        switch (key.getKind())
        {
          case Char:
            char prev_c = key.getChar();
            char c = prev_c;
            if (globalConfig.inverse_numpad)
              c = inverse_numpad_char(c);
            if (map_digit != -1)
            {
              KeyValue modified = ComposeKey.apply(map_digit, c);
              if (modified != null) // Was modified by script
                return modified;
            }
            if (prev_c != c) // Was inverted
              return key.withChar(c);
            return key; // Don't fallback into [modify_key]
        }
        return modify_key(key);
      }
    });
  }

  /** Modify the pin entry layout. [main_kw] is used to map the digits into the
      same script. */
  public static KeyboardData modify_pinentry(KeyboardData kw, KeyboardData main_kw)
  {
    KeyboardData.MapKeyValues m = numpad_script_map(main_kw.numpad_script);
    return m == null ? kw : kw.mapKeys(m);
  }

  /** Modify the number row according to [main_kw]'s script. */
  static KeyboardData.Row modify_number_row(KeyboardData.Row row,
      KeyboardData main_kw)
  {
    KeyboardData.MapKeyValues m = numpad_script_map(main_kw.numpad_script);
    return m == null ? row : row.mapKeys(m);
  }

  static KeyboardData.MapKeyValues numpad_script_map(String numpad_script)
  {
    final int map_digit = KeyModifier.modify_numpad_script(numpad_script);
    if (map_digit == -1)
      return null;
    return new KeyboardData.MapKeyValues() {
      public KeyValue apply(KeyValue key, boolean localized)
      {
        KeyValue modified = ComposeKey.apply(map_digit, key);
        return (modified != null) ? modified : key;
      }
    };
  }

  /** Modify keys on the main layout and on the numpad according to the config.
   */
  static KeyValue modify_key(KeyValue orig)
  {
    return modify_key(orig, false);
  }

  static KeyValue modify_key(KeyValue orig, boolean isPreview)
  {
    EditorConfig ec = globalConfig != null ? globalConfig.editor_config : null;
    switch (orig.getKind())
    {
      case Event:
        switch (orig.getEvent())
        {
          case CHANGE_METHOD_PICKER:
            if (globalConfig != null && globalConfig.change_method_key_replacement != null)
              return globalConfig.change_method_key_replacement;
            return isPreview ? orig : null;
          case ACTION:
            if (ec != null && ec.action_key_replacement != null)
              return ec.action_key_replacement;
            return isPreview ? KeyValue.getKeyByName("enter") : null;
          case SWITCH_FORWARD:
            if (globalConfig != null && globalConfig.layouts != null && globalConfig.layouts.size() > 1)
              return orig;
            return isPreview ? orig : null;
          case SWITCH_BACKWARD:
            if (globalConfig != null && globalConfig.layouts != null && globalConfig.layouts.size() > 2)
              return orig;
            return isPreview ? orig : null;
          case SWITCH_VOICE_TYPING:
          case SWITCH_VOICE_TYPING_CHOOSER:
            if (globalConfig != null && globalConfig.shouldOfferVoiceTyping)
              return orig;
            return isPreview ? orig : null;
        }
        break;
      case Keyevent:
        if (ec != null)
        {
          switch (orig.getKeyevent())
          {
            case KeyEvent.KEYCODE_ENTER:
              if (ec.enter_key_replacement != null)
                return ec.enter_key_replacement;
              break;
          }
        }
        break;
    }
    return orig;
  }

  static char inverse_numpad_char(char c)
  {
    switch (c)
    {
      case '7': return '1';
      case '8': return '2';
      case '9': return '3';
      case '1': return '7';
      case '2': return '8';
      case '3': return '9';
      default: return c;
    }
  }

  public static void init(Config globalConfig_, Resources res)
  {
    globalConfig = globalConfig_;
    try
    {
      number_row_no_symbols = KeyboardData.load_row(res, R.xml.number_row_no_symbols);
      number_row_symbols = KeyboardData.load_row(res, R.xml.number_row);
      bottom_row = KeyboardData.load_row(res, R.xml.bottom_row);
      num_pad = KeyboardData.load_num_pad(res);
      split_middle_column = KeyboardData.load_row(res, R.xml.split_middle_column);
    }
    catch (Exception e)
    {
      throw new RuntimeException(e.getMessage()); // Not recoverable
    }
  }
}
