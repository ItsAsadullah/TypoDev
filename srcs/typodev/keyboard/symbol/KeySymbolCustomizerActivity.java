package typodev.keyboard.symbol;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import typodev.keyboard.Config;
import typodev.keyboard.DeviceLocales;
import typodev.keyboard.DirectBootAwarePreferences;
import typodev.keyboard.KeyboardData;
import typodev.keyboard.KeyValue;
import typodev.keyboard.LayoutModifier;
import typodev.keyboard.R;
import typodev.keyboard.prefs.CustomExtraKeysPreference;
import typodev.keyboard.prefs.ExtraKeysPreference;
import typodev.keyboard.prefs.LayoutsPreference;

public class KeySymbolCustomizerActivity extends Activity
{
  private SharedPreferences _prefs;
  private List<CustomSymbolStore.Mapping> mMappings;

  // Currently selected state
  private String mSelectedKey = "q";
  private int mSelectedPos = 1; // 1 = Top-Left (NW) by default

  // Layout & Live Keyboard state
  private KeyboardData mBaseKeyboardData;
  private KeyboardData mActiveKeyboardData;
  private String mActiveLayoutName = "QWERTY (US)";
  private List<LayoutOption> mLayoutOptions = new ArrayList<LayoutOption>();

  // Reserved keys & Extra palettes
  private List<String> mReservedKeys = new ArrayList<String>();
  private LinearLayout mContainerReservedKeys;
  private Button mBtnClearAllReserved;
  private TextView mTxtReservedKeysHint;
  private LinearLayout mPaletteActiveExtraKeys;
  private LinearLayout mPaletteAllExtraKeys;

  // Views
  private Spinner mSpinnerLayout;
  private TextView mTxtLiveLayoutName;
  private LinearLayout mContainerLiveKeyboard;
  private TextView mTxtSelectedKeyBadge;
  private TextView mTxtTargetIndicator;
  private EditText mEdtSymbolInput;
  private LinearLayout mContainerActiveMappings;

  // 9 Compass Cells, Text & Statuses
  private LinearLayout[] mCells = new LinearLayout[9];
  private TextView[] mCellTexts = new TextView[9];
  private TextView[] mCellStatuses = new TextView[9];

  private float mDensity;

  public static class LayoutOption
  {
    public final String id;
    public final String displayName;

    public LayoutOption(String id, String displayName)
    {
      this.id = id;
      this.displayName = displayName;
    }

    @Override
    public String toString()
    {
      return displayName;
    }
  }

  private static final String[] PALETTE_CURRENCY = new String[]{
      "৳", "$", "€", "£", "₹", "¥", "¢", "₿", "৲"
  };

  private static final String[] PALETTE_MATH = new String[]{
      "@", "#", "&", "*", "?", "!", "%", "+", "-", "=", "~", "^", "|", "_", "°", "±", "×", "÷"
  };

  private static final String[] PALETTE_BENGALI = new String[]{
      "১", "২", "৩", "৪", "৫", "৬", "৭", "৮", "৯", "০", "৳", "৹", "ঽ", "৻"
  };

  private static final String[] PALETTE_TECH = new String[]{
      "•", "★", "✓", "✕", "©", "®", "™", "/", "\\", ":", ";", "\"", "'", "(", ")", "[", "]", "{", "}", "<", ">"
  };

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_symbol_customizer);

    mDensity = getResources().getDisplayMetrics().density;
    _prefs = PreferenceManager.getDefaultSharedPreferences(this);
    mMappings = CustomSymbolStore.getMappings(_prefs);
    mReservedKeys = CustomSymbolStore.getReservedKeys(_prefs);

    // Ensure global configuration and layout modifier are fully initialized
    if (Config.globalConfig() == null)
    {
      Config.initGlobalConfig(_prefs, getResources(), false, null);
    }
    else
    {
      LayoutModifier.init(Config.globalConfig(), getResources());
    }

    if (Config.globalConfig() != null && Config.globalConfig().extra_keys_subtype == null)
    {
      try
      {
        DeviceLocales dl = DeviceLocales.load(this);
        Config.globalConfig().extra_keys_subtype = dl.extra_keys();
      }
      catch (Throwable ignored)
      {
      }
    }

    initViews();
    initLayoutPicker();
    buildPalettes();
    renderReservedKeys();
    buildActiveMappingsList();
  }

  @Override
  protected void onStop()
  {
    if (_prefs != null)
      DirectBootAwarePreferences.copy_preferences_to_protected_storage(this, _prefs);
    super.onStop();
  }

  private void initViews()
  {
    findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        finish();
      }
    });

    mSpinnerLayout = findViewById(R.id.spinner_layout_picker);
    mTxtLiveLayoutName = findViewById(R.id.txt_live_layout_name);
    mContainerLiveKeyboard = findViewById(R.id.container_live_keyboard);
    mTxtSelectedKeyBadge = findViewById(R.id.txt_selected_key_badge);
    mTxtTargetIndicator = findViewById(R.id.txt_target_indicator);
    mEdtSymbolInput = findViewById(R.id.edt_symbol_input);
    mContainerActiveMappings = findViewById(R.id.container_active_mappings);

    mContainerReservedKeys = findViewById(R.id.container_reserved_keys);
    mBtnClearAllReserved = findViewById(R.id.btn_clear_all_reserved);
    mTxtReservedKeysHint = findViewById(R.id.txt_reserved_keys_hint);
    mPaletteActiveExtraKeys = findViewById(R.id.palette_active_extra_keys);
    mPaletteAllExtraKeys = findViewById(R.id.palette_all_extra_keys);

    if (mBtnClearAllReserved != null)
    {
      mBtnClearAllReserved.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          mReservedKeys.clear();
          CustomSymbolStore.saveReservedKeys(_prefs, mReservedKeys);
          renderReservedKeys();
          Toast.makeText(KeySymbolCustomizerActivity.this, "Cleared all reserved keys", Toast.LENGTH_SHORT).show();
        }
      });
    }

    // Compass cells setup
    setupCompassCell(1, R.id.cell_pos_1, R.id.txt_pos_1, R.id.txt_status_1);
    setupCompassCell(7, R.id.cell_pos_7, R.id.txt_pos_7, R.id.txt_status_7);
    setupCompassCell(2, R.id.cell_pos_2, R.id.txt_pos_2, R.id.txt_status_2);
    setupCompassCell(5, R.id.cell_pos_5, R.id.txt_pos_5, R.id.txt_status_5);
    setupCompassCell(0, R.id.cell_pos_0, R.id.txt_pos_0, R.id.txt_status_0);
    setupCompassCell(6, R.id.cell_pos_6, R.id.txt_pos_6, R.id.txt_status_6);
    setupCompassCell(3, R.id.cell_pos_3, R.id.txt_pos_3, R.id.txt_status_3);
    setupCompassCell(8, R.id.cell_pos_8, R.id.txt_pos_8, R.id.txt_status_8);
    setupCompassCell(4, R.id.cell_pos_4, R.id.txt_pos_4, R.id.txt_status_4);

    findViewById(R.id.btn_custom_key_dialog).setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        showCustomKeyInputDialog();
      }
    });

    findViewById(R.id.btn_assign_symbol).setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        String sym = mEdtSymbolInput.getText().toString().trim();
        if (sym.isEmpty())
        {
          Toast.makeText(KeySymbolCustomizerActivity.this, "Please select or type a symbol", Toast.LENGTH_SHORT).show();
          return;
        }
        assignSymbol(sym);
      }
    });

    findViewById(R.id.btn_clear_slot).setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        clearSelectedSlot();
      }
    });

    findViewById(R.id.btn_reset_all).setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        showResetAllConfirmDialog();
      }
    });

    View.OnClickListener saveListener = new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        saveAndApply();
      }
    };
    findViewById(R.id.btn_save_top).setOnClickListener(saveListener);
    findViewById(R.id.btn_save_bottom).setOnClickListener(saveListener);
  }

  private void setupCompassCell(final int pos, int cellResId, int textResId, int statusResId)
  {
    mCells[pos] = findViewById(cellResId);
    mCellTexts[pos] = findViewById(textResId);
    mCellStatuses[pos] = findViewById(statusResId);
    if (mCells[pos] != null)
    {
      mCells[pos].setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          mSelectedPos = pos;
          updateCompassGrid();
        }
      });

      if (pos != 0) // Extra keys / swipe positions 1..8
      {
        mCells[pos].setOnLongClickListener(new View.OnLongClickListener()
        {
          @Override
          public boolean onLongClick(View v)
          {
            String sym = getEffectiveSymbolAt(mSelectedKey, pos);
            if (sym == null || sym.isEmpty() || CustomSymbolStore.SYMBOL_EMPTY.equalsIgnoreCase(sym))
            {
              Toast.makeText(KeySymbolCustomizerActivity.this, "This slot is empty", Toast.LENGTH_SHORT).show();
              return false;
            }

            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            ClipData.Item item = new ClipData.Item(String.valueOf(pos));
            ClipData dragData = new ClipData("compass_cell", new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN}, item);
            View.DragShadowBuilder shadow = new View.DragShadowBuilder(v);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            {
              v.startDragAndDrop(dragData, shadow, pos, 0);
            }
            else
            {
              v.startDrag(dragData, shadow, pos, 0);
            }
            return true;
          }
        });

        mCells[pos].setOnDragListener(new View.OnDragListener()
        {
          @Override
          public boolean onDrag(View v, DragEvent event)
          {
            switch (event.getAction())
            {
              case DragEvent.ACTION_DRAG_STARTED:
                return true;

              case DragEvent.ACTION_DRAG_ENTERED:
                highlightCellForDrop(pos, true);
                return true;

              case DragEvent.ACTION_DRAG_LOCATION:
                return true;

              case DragEvent.ACTION_DRAG_EXITED:
                highlightCellForDrop(pos, false);
                return true;

              case DragEvent.ACTION_DROP:
                highlightCellForDrop(pos, false);
                int fromPos = -1;
                if (event.getLocalState() instanceof Integer)
                {
                  fromPos = (Integer) event.getLocalState();
                }
                else if (event.getClipData() != null && event.getClipData().getItemCount() > 0)
                {
                  try
                  {
                    fromPos = Integer.parseInt(event.getClipData().getItemAt(0).getText().toString());
                  }
                  catch (Exception ignored) {}
                }

                if (fromPos > 0 && fromPos != pos)
                {
                  swapOrMoveCompassCells(fromPos, pos);
                }
                return true;

              case DragEvent.ACTION_DRAG_ENDED:
                highlightCellForDrop(pos, false);
                return true;
            }
            return false;
          }
        });
      }
    }
  }

  private void highlightCellForDrop(int pos, boolean highlight)
  {
    if (pos < 0 || pos >= 9) return;
    LinearLayout cell = mCells[pos];
    if (cell == null) return;

    GradientDrawable gd = new GradientDrawable();
    gd.setCornerRadius(8 * mDensity);
    if (highlight)
    {
      gd.setColor(Color.parseColor("#064E3B")); // Deep emerald
      gd.setStroke(Math.round(2.5f * mDensity), Color.parseColor("#34D399")); // Glowing bright emerald
    }
    else
    {
      boolean isSelected = (pos == mSelectedPos);
      if (isSelected)
      {
        gd.setColor(Color.parseColor("#1E3A8A"));
        gd.setStroke(Math.round(2 * mDensity), Color.parseColor("#60A5FA"));
      }
      else if (pos == 0)
      {
        gd.setColor(Color.parseColor("#374151"));
        gd.setStroke(Math.round(1 * mDensity), Color.parseColor("#4B5563"));
      }
      else
      {
        gd.setColor(Color.parseColor("#1F2937"));
        gd.setStroke(Math.round(1 * mDensity), Color.parseColor("#374151"));
      }
    }
    cell.setBackground(gd);
  }

  public String getEffectiveSymbolAt(String baseKey, int pos)
  {
    String custom = getCustomMappingFor(baseKey, pos);
    if (custom != null)
    {
      if (CustomSymbolStore.SYMBOL_EMPTY.equalsIgnoreCase(custom) || "none".equalsIgnoreCase(custom))
      {
        return null;
      }
      return custom;
    }
    KeyboardData.Key baseKeyObj = findKeyInBaseLayout(baseKey);
    if (baseKeyObj != null && baseKeyObj.keys != null && pos < baseKeyObj.keys.length && baseKeyObj.keys[pos] != null)
    {
      return CustomSymbolStore.getCanonicalCodeForKeyValue(baseKeyObj.keys[pos]);
    }
    return null;
  }

  public void swapOrMoveCompassCells(int fromPos, int toPos)
  {
    if (fromPos <= 0 || toPos <= 0 || fromPos == toPos) return;

    String symFrom = getEffectiveSymbolAt(mSelectedKey, fromPos);
    String symTo = getEffectiveSymbolAt(mSelectedKey, toPos);

    if (symFrom == null || symFrom.isEmpty() || CustomSymbolStore.SYMBOL_EMPTY.equalsIgnoreCase(symFrom))
    {
      Toast.makeText(this, "Source slot is empty", Toast.LENGTH_SHORT).show();
      return;
    }

    KeyboardData.Key baseKeyObj = findKeyInBaseLayout(mSelectedKey);

    // Remove any existing mappings for fromPos and toPos
    for (int i = mMappings.size() - 1; i >= 0; i--)
    {
      CustomSymbolStore.Mapping m = mMappings.get(i);
      if (m != null && m.baseKey != null && mSelectedKey != null
          && m.baseKey.equalsIgnoreCase(mSelectedKey) && (m.pos == fromPos || m.pos == toPos))
      {
        mMappings.remove(i);
      }
    }

    if (symTo != null && !symTo.isEmpty() && !CustomSymbolStore.SYMBOL_EMPTY.equalsIgnoreCase(symTo))
    {
      // Both had symbols: SWAP!
      mMappings.add(new CustomSymbolStore.Mapping(mSelectedKey, toPos, symFrom));
      mMappings.add(new CustomSymbolStore.Mapping(mSelectedKey, fromPos, symTo));

      String lblFrom = CustomSymbolStore.getFriendlyActionLabel(symFrom);
      String lblTo = CustomSymbolStore.getFriendlyActionLabel(symTo);
      Toast.makeText(this, "Swapped [" + lblFrom + "] and [" + lblTo + "]", Toast.LENGTH_SHORT).show();
    }
    else
    {
      // toPos was empty: MOVE!
      mMappings.add(new CustomSymbolStore.Mapping(mSelectedKey, toPos, symFrom));

      // fromPos is now empty. If base layout had a symbol at fromPos, mark as __EMPTY__
      boolean baseHadSymbolAtFrom = (baseKeyObj != null && baseKeyObj.keys != null
          && fromPos < baseKeyObj.keys.length && baseKeyObj.keys[fromPos] != null);
      if (baseHadSymbolAtFrom)
      {
        mMappings.add(new CustomSymbolStore.Mapping(mSelectedKey, fromPos, CustomSymbolStore.SYMBOL_EMPTY));
      }

      String lblFrom = CustomSymbolStore.getFriendlyActionLabel(symFrom);
      String toName = CustomSymbolStore.getPosName(toPos);
      Toast.makeText(this, "Moved [" + lblFrom + "] to " + toName + " (" + CustomSymbolStore.getPosIcon(toPos) + ")", Toast.LENGTH_SHORT).show();
    }

    // Select destination position
    mSelectedPos = toPos;

    // Apply to active keyboard and re-render
    mActiveKeyboardData = LayoutModifier.apply_custom_symbols(mBaseKeyboardData, mMappings);
    renderLiveKeyboard();
    updateCompassGrid();
    buildActiveMappingsList();
  }

  private void initLayoutPicker()
  {
    mLayoutOptions.clear();
    Set<String> addedIds = new HashSet<String>();

    // Add popular defaults first
    addLayoutOption("latn_qwerty_us", "QWERTY (US)", addedIds);
    addLayoutOption("beng_avro", "Bengali - Avro Phonetic (অভ্র)", addedIds);
    addLayoutOption("beng_national", "Bengali - National / Bijoy (জাতীয় / ই-বিজয়)", addedIds);
    addLayoutOption("beng_provat", "Bengali - Provat (প্রভাত)", addedIds);
    addLayoutOption("latn_colemak", "Colemak", addedIds);
    addLayoutOption("latn_dvorak", "Dvorak", addedIds);
    addLayoutOption("arab_pc", "Arabic PC", addedIds);

    // Add remaining system layouts from arrays
    try
    {
      String[] ids = getResources().getStringArray(R.array.pref_layout_values);
      String[] names = getResources().getStringArray(R.array.pref_layout_entries);
      if (ids != null && names != null)
      {
        int count = Math.min(ids.length, names.length);
        for (int i = 0; i < count; i++)
        {
          String id = ids[i];
          String name = names[i];
          if (id != null && !id.equals("system") && !id.equals("custom") && !addedIds.contains(id))
          {
            if (name.contains("অভ্র") || name.contains("Avro"))
            {
              name = "Bengali - Avro Phonetic (অভ্র)";
            }
            else if (name.contains("জাতীয়") || name.contains("জাতীয়"))
            {
              name = "Bengali - National (Jatiya)";
            }
            else if (name.contains("প্রভাত"))
            {
              name = "Bengali - Provat";
            }
            addLayoutOption(id, name, addedIds);
          }
        }
      }
    }
    catch (Throwable ignored)
    {
    }

    ArrayAdapter<LayoutOption> adapter = new ArrayAdapter<LayoutOption>(this,
        android.R.layout.simple_spinner_item, mLayoutOptions)
    {
      @Override
      public View getView(int position, View convertView, ViewGroup parent)
      {
        TextView v = (TextView) super.getView(position, convertView, parent);
        v.setTextColor(Color.parseColor("#60A5FA"));
        v.setTypeface(null, Typeface.BOLD);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        return v;
      }

      @Override
      public View getDropDownView(int position, View convertView, ViewGroup parent)
      {
        TextView v = (TextView) super.getDropDownView(position, convertView, parent);
        v.setBackgroundColor(Color.parseColor("#1F2937"));
        v.setTextColor(Color.WHITE);
        v.setPadding(Math.round(14 * mDensity), Math.round(10 * mDensity), Math.round(14 * mDensity), Math.round(10 * mDensity));
        return v;
      }
    };
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    mSpinnerLayout.setAdapter(adapter);

    mSpinnerLayout.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
    {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
      {
        if (position >= 0 && position < mLayoutOptions.size())
        {
          LayoutOption opt = mLayoutOptions.get(position);
          switchLayout(opt);
        }
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent)
      {
      }
    });

    if (!mLayoutOptions.isEmpty())
    {
      switchLayout(mLayoutOptions.get(0));
    }
  }

  private void addLayoutOption(String id, String name, Set<String> addedIds)
  {
    if (id != null && !addedIds.contains(id))
    {
      addedIds.add(id);
      mLayoutOptions.add(new LayoutOption(id, name));
    }
  }

  private void switchLayout(LayoutOption opt)
  {
    if (opt == null) return;
    try
    {
      if (Config.globalConfig() != null)
      {
        Config.globalConfig().extra_keys_param = ExtraKeysPreference.get_extra_keys(_prefs);
        Config.globalConfig().extra_keys_custom = CustomExtraKeysPreference.get(_prefs);
      }

      KeyboardData rawKd = LayoutsPreference.layout_of_string(getResources(), opt.id);
      if (rawKd == null)
      {
        rawKd = KeyboardData.load(getResources(), R.xml.latn_qwerty_us);
      }

      // Generate base layout containing bottom_row, number_row, AND all extra keys!
      mBaseKeyboardData = LayoutModifier.modify_layout_for_preview(rawKd);
      if (mBaseKeyboardData == null)
      {
        mBaseKeyboardData = rawKd;
      }

      // Generate active layout with custom symbol overrides
      mActiveKeyboardData = LayoutModifier.apply_custom_symbols(mBaseKeyboardData, mMappings);
    }
    catch (Throwable t)
    {
      try
      {
        KeyboardData rawKd = KeyboardData.load(getResources(), R.xml.latn_qwerty_us);
        mBaseKeyboardData = LayoutModifier.modify_layout_for_preview(rawKd);
        mActiveKeyboardData = LayoutModifier.apply_custom_symbols(mBaseKeyboardData, mMappings);
      }
      catch (Throwable ignored)
      {
      }
    }

    mActiveLayoutName = opt.displayName;
    if (mTxtLiveLayoutName != null)
    {
      mTxtLiveLayoutName.setText(mActiveLayoutName);
    }

    // Retain selected key if in layout, otherwise pick first key
    if (findKeyInBaseLayout(mSelectedKey) == null)
    {
      String firstKey = findFirstKeyInLayout(mActiveKeyboardData);
      if (firstKey != null)
      {
        mSelectedKey = firstKey.toLowerCase().trim();
      }
    }

    renderLiveKeyboard();
    updateCompassGrid();
  }

  private String findFirstKeyInLayout(KeyboardData data)
  {
    if (data == null || data.rows == null) return "q";
    for (KeyboardData.Row r : data.rows)
    {
      if (r == null || r.keys == null) continue;
      for (KeyboardData.Key k : r.keys)
      {
        String base = getBaseKeyName(k);
        if (base != null && !base.isEmpty() && !base.equalsIgnoreCase("space")
            && !base.equalsIgnoreCase("ctrl") && !base.equalsIgnoreCase("fn")
            && !base.equalsIgnoreCase("shift") && !base.equalsIgnoreCase("compose"))
        {
          return base;
        }
      }
    }
    return "q";
  }

  private String getBaseKeyName(KeyboardData.Key key)
  {
    if (key == null) return null;
    if (key.role == KeyboardData.Key.Role.Space_bar)
      return "space";
    if (key.keys != null && key.keys.length > 0 && key.keys[0] != null)
    {
      KeyValue kv = key.keys[0];
      if (kv.getKind() == KeyValue.Kind.Modifier)
      {
        switch (kv.getModifier())
        {
          case SHIFT: return "shift";
          case CTRL: return "ctrl";
          case FN: return "fn";
          case ALT: return "alt";
          case META: return "meta";
          default: break;
        }
      }
      if (kv.getKind() == KeyValue.Kind.Compose_pending)
      {
        return "compose";
      }
      if (kv.getKind() == KeyValue.Kind.Editing)
      {
        switch (kv.getEditing())
        {
          case SPACE_BAR: return "space";
          case BACKSPACE: return "backspace";
          default: break;
        }
      }
      if (kv.getKind() == KeyValue.Kind.Keyevent)
      {
        switch (kv.getKeyevent())
        {
          case android.view.KeyEvent.KEYCODE_ENTER: return "enter";
          case android.view.KeyEvent.KEYCODE_ESCAPE: return "esc";
          case android.view.KeyEvent.KEYCODE_TAB: return "tab";
          case android.view.KeyEvent.KEYCODE_DEL: return "backspace";
          case android.view.KeyEvent.KEYCODE_FORWARD_DEL: return "delete";
          default: break;
        }
      }
      if (kv.getKind() == KeyValue.Kind.Event && kv.getEvent() == KeyValue.Event.ACTION)
      {
        return "enter";
      }
      String s = kv.getString();
      if (s != null && !s.trim().isEmpty())
        return s.trim();
    }
    return null;
  }

  private boolean isKeySelected(KeyboardData.Key key)
  {
    String base = getBaseKeyName(key);
    return base != null && base.equalsIgnoreCase(mSelectedKey);
  }

  private boolean hasCustomMapping(KeyboardData.Key key)
  {
    String base = getBaseKeyName(key);
    if (base == null || mMappings == null) return false;
    for (CustomSymbolStore.Mapping m : mMappings)
    {
      if (m != null && m.baseKey != null && m.baseKey.equalsIgnoreCase(base))
        return true;
    }
    return false;
  }

  private String getCustomMappingFor(String baseKey, int pos)
  {
    if (baseKey == null || mMappings == null) return null;
    for (CustomSymbolStore.Mapping m : mMappings)
    {
      if (m != null && m.baseKey != null && m.baseKey.equalsIgnoreCase(baseKey) && m.pos == pos)
      {
        return m.symbol;
      }
    }
    return null;
  }

  private KeyboardData.Key findKeyInLayout(String baseKey)
  {
    if (mActiveKeyboardData == null || mActiveKeyboardData.rows == null || baseKey == null) return null;
    for (KeyboardData.Row row : mActiveKeyboardData.rows)
    {
      if (row == null || row.keys == null) continue;
      for (KeyboardData.Key key : row.keys)
      {
        String b = getBaseKeyName(key);
        if (b != null && b.equalsIgnoreCase(baseKey))
          return key;
      }
    }
    return null;
  }

  private KeyboardData.Key findKeyInBaseLayout(String baseKey)
  {
    if (mBaseKeyboardData == null || mBaseKeyboardData.rows == null || baseKey == null) return null;
    for (KeyboardData.Row row : mBaseKeyboardData.rows)
    {
      if (row == null || row.keys == null) continue;
      for (KeyboardData.Key key : row.keys)
      {
        String b = getBaseKeyName(key);
        if (b != null && b.equalsIgnoreCase(baseKey))
          return key;
      }
    }
    return null;
  }

  private void renderLiveKeyboard()
  {
    if (mContainerLiveKeyboard == null || mActiveKeyboardData == null || mActiveKeyboardData.rows == null)
      return;
    mContainerLiveKeyboard.removeAllViews();

    for (KeyboardData.Row row : mActiveKeyboardData.rows)
    {
      if (row == null || row.keys == null) continue;

      LinearLayout rowLayout = new LinearLayout(this);
      rowLayout.setOrientation(LinearLayout.HORIZONTAL);
      LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.MATCH_PARENT,
          LinearLayout.LayoutParams.WRAP_CONTENT);
      rowLp.topMargin = Math.round(1.5f * mDensity);
      rowLp.bottomMargin = Math.round(1.5f * mDensity);
      rowLayout.setLayoutParams(rowLp);

      // Compute total weight of this row to ensure keys expand and fill 100% of the screen width
      float rowTotalWeight = 0f;
      if (row.shift > 0.001f) rowTotalWeight += row.shift;
      for (KeyboardData.Key k : row.keys)
      {
        if (k == null) continue;
        if (k.shift > 0.001f) rowTotalWeight += k.shift;
        rowTotalWeight += (k.width > 0.01f) ? k.width : 1.0f;
      }
      if (rowTotalWeight > 0.01f)
      {
        rowLayout.setWeightSum(rowTotalWeight);
      }

      // Spacer before row if shifted
      if (row.shift > 0.01f)
      {
        View spacer = new View(this);
        LinearLayout.LayoutParams spLp = new LinearLayout.LayoutParams(0, 1);
        spLp.weight = row.shift;
        spacer.setLayoutParams(spLp);
        rowLayout.addView(spacer);
      }

      for (final KeyboardData.Key key : row.keys)
      {
        if (key == null) continue;

        // Spacer before key if shifted
        if (key.shift > 0.01f)
        {
          View spacer = new View(this);
          LinearLayout.LayoutParams spLp = new LinearLayout.LayoutParams(0, 1);
          spLp.weight = key.shift;
          spacer.setLayoutParams(spLp);
          rowLayout.addView(spacer);
        }

        View keyView = createLiveKeyView(key);
        int keyHeightDp = 48; // Spacious, comfortable keyboard height
        LinearLayout.LayoutParams keyLp = new LinearLayout.LayoutParams(0, Math.round(keyHeightDp * mDensity));
        keyLp.weight = (key.width > 0.01f) ? key.width : 1.0f;
        keyLp.leftMargin = Math.round(1f * mDensity);
        keyLp.rightMargin = Math.round(1f * mDensity);
        keyView.setLayoutParams(keyLp);

        rowLayout.addView(keyView);
      }

      mContainerLiveKeyboard.addView(rowLayout);
    }
  }

  private View createLiveKeyView(final KeyboardData.Key key)
  {
    RelativeLayout keyLayout = new RelativeLayout(this);
    keyLayout.setClickable(true);

    final String base = getBaseKeyName(key);
    boolean isSelected = isKeySelected(key);
    boolean isCustom = hasCustomMapping(key);

    GradientDrawable gd = new GradientDrawable();
    gd.setCornerRadius(6 * mDensity);

    if (isSelected)
    {
      gd.setColor(Color.parseColor("#1E3A8A"));
      gd.setStroke(Math.round(2 * mDensity), Color.parseColor("#60A5FA"));
    }
    else if (isCustom)
    {
      gd.setColor(Color.parseColor("#064E3B"));
      gd.setStroke(Math.round(1.5f * mDensity), Color.parseColor("#10B981"));
    }
    else if (key.role == KeyboardData.Key.Role.Action)
    {
      gd.setColor(Color.parseColor("#1E293B"));
      gd.setStroke(Math.round(1 * mDensity), Color.parseColor("#374151"));
    }
    else
    {
      gd.setColor(Color.parseColor("#1F2937"));
      gd.setStroke(Math.round(1 * mDensity), Color.parseColor("#374151"));
    }
    keyLayout.setBackground(gd);

    // Main Center Label
    TextView txtCenter = new TextView(this);
    String centerLabel = formatKeyCenterLabel(key);
    txtCenter.setText(centerLabel);
    txtCenter.setTextSize(TypedValue.COMPLEX_UNIT_SP, (centerLabel.length() > 3) ? 10 : 13);
    txtCenter.setTypeface(null, Typeface.BOLD);
    txtCenter.setTextColor(isSelected ? Color.WHITE : (isCustom ? Color.parseColor("#A7F3D0") : Color.parseColor("#F3F4F6")));
    RelativeLayout.LayoutParams centerLp = new RelativeLayout.LayoutParams(
        RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
    centerLp.addRule(RelativeLayout.CENTER_IN_PARENT);
    txtCenter.setLayoutParams(centerLp);
    keyLayout.addView(txtCenter);

    // Corner Sublabels (NW=1, NE=2, SW=3, SE=4) and Cardinal (N=7, S=8, W=5, E=6)
    if (base != null)
    {
      addCornerBadge(keyLayout, key, base, 1, RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.ALIGN_PARENT_START);
      addCornerBadge(keyLayout, key, base, 2, RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.ALIGN_PARENT_END);
      addCornerBadge(keyLayout, key, base, 3, RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.ALIGN_PARENT_START);
      addCornerBadge(keyLayout, key, base, 4, RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.ALIGN_PARENT_END);

      // Add cardinal badges if present (for example on Arrow navigation key or Space bar)
      if (hasSymbolAt(key, base, 7))
        addCornerBadge(keyLayout, key, base, 7, RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.CENTER_HORIZONTAL);
      if (hasSymbolAt(key, base, 8))
        addCornerBadge(keyLayout, key, base, 8, RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.CENTER_HORIZONTAL);
      if (hasSymbolAt(key, base, 5))
        addCornerBadge(keyLayout, key, base, 5, RelativeLayout.CENTER_VERTICAL, RelativeLayout.ALIGN_PARENT_START);
      if (hasSymbolAt(key, base, 6))
        addCornerBadge(keyLayout, key, base, 6, RelativeLayout.CENTER_VERTICAL, RelativeLayout.ALIGN_PARENT_END);
    }

    keyLayout.setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        if (base != null && !base.isEmpty())
        {
          selectKey(base);
        }
      }
    });

    return keyLayout;
  }

  private boolean hasSymbolAt(KeyboardData.Key key, String baseKey, int pos)
  {
    String custom = getCustomMappingFor(baseKey, pos);
    if (custom != null)
    {
      if (CustomSymbolStore.SYMBOL_EMPTY.equalsIgnoreCase(custom) || "none".equalsIgnoreCase(custom))
        return false;
      return true;
    }
    return (key != null && key.keys != null && pos < key.keys.length && key.keys[pos] != null);
  }

  private void addCornerBadge(RelativeLayout parent, KeyboardData.Key key, String baseKey, int pos, int verticalRule, int horizontalRule)
  {
    String customSym = getCustomMappingFor(baseKey, pos);
    if (customSym != null && (CustomSymbolStore.SYMBOL_EMPTY.equalsIgnoreCase(customSym) || "none".equalsIgnoreCase(customSym)))
    {
      return; // Explicitly cleared by user
    }

    String defSym = null;
    if (key.keys != null && pos < key.keys.length && key.keys[pos] != null)
    {
      defSym = CustomSymbolStore.formatKeyShortBadge(key.keys[pos]);
    }

    if (customSym == null && (defSym == null || defSym.isEmpty())) return;

    TextView badge = new TextView(this);
    badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8f);
    badge.setTypeface(null, Typeface.BOLD);
    badge.setPadding(Math.round(2 * mDensity), Math.round(1 * mDensity), Math.round(2 * mDensity), Math.round(1 * mDensity));

    if (customSym != null)
    {
      String label = CustomSymbolStore.isSpecialAction(customSym) ?
          getShortSpecialIcon(customSym) : customSym;
      badge.setText(label);
      badge.setTextColor(Color.parseColor("#50FA7B")); // Emerald green for custom
    }
    else
    {
      badge.setText(defSym);
      badge.setTextColor(Color.parseColor("#93C5FD")); // Crisp sky blue for default & extra keys
    }

    RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
        RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
    lp.addRule(verticalRule);
    lp.addRule(horizontalRule);
    badge.setLayoutParams(lp);

    parent.addView(badge);
  }

  private String getShortSpecialIcon(String code)
  {
    for (CustomSymbolStore.SpecialAction sa : CustomSymbolStore.SPECIAL_ACTIONS)
    {
      if (sa.code.equalsIgnoreCase(code))
        return sa.icon;
    }
    return code;
  }

  private String formatKeyCenterLabel(KeyboardData.Key key)
  {
    if (key.role == KeyboardData.Key.Role.Space_bar)
    {
      String label = typodev.keyboard.Keyboard2View.getLayoutDisplayName(mActiveKeyboardData);
      return "␣ " + label;
    }
    String base = getBaseKeyName(key);
    if (base != null)
    {
      if (base.equalsIgnoreCase("shift")) return "⇧ Shift";
      if (base.equalsIgnoreCase("ctrl")) return "Ctrl";
      if (base.equalsIgnoreCase("fn")) return "Fn";
      if (base.equalsIgnoreCase("compose")) return "⬍⬄ Nav";
      if (base.equalsIgnoreCase("backspace")) return "⌫ Del";
      if (base.equalsIgnoreCase("enter")) return "↵ Enter";
      if (base.equalsIgnoreCase("tab")) return "⇥ Tab";
      if (base.equalsIgnoreCase("esc")) return "Esc";
      if (base.equalsIgnoreCase("space")) return "␣ Space";
      return base.toUpperCase();
    }
    return "";
  }

  private void selectKey(String keyName)
  {
    mSelectedKey = keyName.toLowerCase().trim();
    renderLiveKeyboard();
    updateCompassGrid();
  }

  private void showCustomKeyInputDialog()
  {
    final EditText input = new EditText(this);
    input.setHint("e.g. ক or z or / or space");
    input.setTextColor(Color.WHITE);
    input.setHintTextColor(Color.GRAY);

    new AlertDialog.Builder(this)
        .setTitle("Type Specific Key")
        .setMessage("Enter the base key character to customize:")
        .setView(input)
        .setPositiveButton("Select", new DialogInterface.OnClickListener()
        {
          @Override
          public void onClick(DialogInterface dialog, int which)
          {
            String k = input.getText().toString().trim();
            if (!k.isEmpty())
            {
              selectKey(k);
            }
          }
        })
        .setNegativeButton("Cancel", null)
        .show();
  }

  private void buildPalettes()
  {
    populateActiveExtraKeysPalette();
    populateSpecialActionsPalette();
    populateAllExtraKeysPalette();
    populatePalette(R.id.palette_currency, PALETTE_CURRENCY);
    populatePalette(R.id.palette_math, PALETTE_MATH);
    populatePalette(R.id.palette_bengali, PALETTE_BENGALI);
    populatePalette(R.id.palette_tech, PALETTE_TECH);
  }

  private void populateActiveExtraKeysPalette()
  {
    LinearLayout container = findViewById(R.id.palette_active_extra_keys);
    if (container == null) return;
    container.removeAllViews();

    List<String> activeKeys = new ArrayList<String>();
    for (String keyName : ExtraKeysPreference.extra_keys)
    {
      if (_prefs.getBoolean(ExtraKeysPreference.pref_key_of_key_name(keyName),
          ExtraKeysPreference.default_checked(keyName)))
      {
        activeKeys.add(keyName);
      }
    }

    if (activeKeys.isEmpty())
    {
      TextView emptyTxt = new TextView(this);
      emptyTxt.setText("No extra keys active in Settings. Turn on keys in 'Add keys to the keyboard'.");
      emptyTxt.setTextColor(Color.parseColor("#64748B"));
      emptyTxt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
      emptyTxt.setPadding(0, Math.round(4 * mDensity), 0, Math.round(4 * mDensity));
      container.addView(emptyTxt);
      return;
    }

    for (final String keyName : activeKeys)
    {
      TextView chip = new TextView(this);
      chip.setText(CustomSymbolStore.getFriendlyActionLabel(keyName));
      chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
      chip.setTypeface(null, Typeface.BOLD);
      chip.setGravity(Gravity.CENTER);
      chip.setTextColor(Color.parseColor("#34D399")); // Emerald green
      chip.setPadding(Math.round(11 * mDensity), Math.round(7 * mDensity), Math.round(11 * mDensity), Math.round(7 * mDensity));

      GradientDrawable gd = new GradientDrawable();
      gd.setColor(Color.parseColor("#064E3B")); // Dark emerald
      gd.setCornerRadius(6 * mDensity);
      gd.setStroke(Math.round(1 * mDensity), Color.parseColor("#10B981"));
      chip.setBackground(gd);

      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
      lp.setMargins(0, 0, Math.round(6 * mDensity), 0);
      chip.setLayoutParams(lp);

      chip.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          mEdtSymbolInput.setText(keyName);
          assignSymbol(keyName);
        }
      });

      container.addView(chip);
    }
  }

  private void populateAllExtraKeysPalette()
  {
    LinearLayout container = findViewById(R.id.palette_all_extra_keys);
    if (container == null) return;
    container.removeAllViews();

    for (final String keyName : ExtraKeysPreference.extra_keys)
    {
      TextView chip = new TextView(this);
      chip.setText(CustomSymbolStore.getFriendlyActionLabel(keyName));
      chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
      chip.setTypeface(null, Typeface.BOLD);
      chip.setGravity(Gravity.CENTER);
      chip.setTextColor(Color.parseColor("#C4B5FD")); // Soft violet
      chip.setPadding(Math.round(11 * mDensity), Math.round(7 * mDensity), Math.round(11 * mDensity), Math.round(7 * mDensity));

      GradientDrawable gd = new GradientDrawable();
      gd.setColor(Color.parseColor("#2E1065")); // Dark violet
      gd.setCornerRadius(6 * mDensity);
      gd.setStroke(Math.round(1 * mDensity), Color.parseColor("#8B5CF6"));
      chip.setBackground(gd);

      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
      lp.setMargins(0, 0, Math.round(6 * mDensity), 0);
      chip.setLayoutParams(lp);

      chip.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          mEdtSymbolInput.setText(keyName);
          assignSymbol(keyName);
        }
      });

      container.addView(chip);
    }
  }

  private void renderReservedKeys()
  {
    if (mContainerReservedKeys == null) return;
    mContainerReservedKeys.removeAllViews();

    if (mReservedKeys == null || mReservedKeys.isEmpty())
    {
      if (mBtnClearAllReserved != null) mBtnClearAllReserved.setVisibility(View.GONE);
      if (mTxtReservedKeysHint != null)
      {
        mTxtReservedKeysHint.setText("Keys cleared from the keyboard are kept here. Tap any key & corner above, then tap a reserved key below to move it there!");
      }
      TextView emptyTxt = new TextView(this);
      emptyTxt.setText("No reserved keys currently. When you Clear a key or action from the keyboard (e.g. Tab, Select All), it will be kept here for repositioning!");
      emptyTxt.setTextColor(Color.parseColor("#64748B"));
      emptyTxt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
      emptyTxt.setPadding(0, Math.round(4 * mDensity), 0, Math.round(4 * mDensity));
      mContainerReservedKeys.addView(emptyTxt);
      return;
    }

    if (mBtnClearAllReserved != null) mBtnClearAllReserved.setVisibility(View.VISIBLE);
    String displayKey = mSelectedKey.equals("space") ? "Space" : mSelectedKey.toUpperCase();
    if (mTxtReservedKeysHint != null)
    {
      mTxtReservedKeysHint.setText("Tap a reserved key below to place on Key [" + displayKey + "] (" + CustomSymbolStore.getPosIcon(mSelectedPos) + "). Tap ✕ to discard.");
    }

    for (int i = 0; i < mReservedKeys.size(); i++)
    {
      final String code = mReservedKeys.get(i);
      final int keyIndex = i;

      LinearLayout chip = new LinearLayout(this);
      chip.setOrientation(LinearLayout.HORIZONTAL);
      chip.setGravity(Gravity.CENTER_VERTICAL);
      chip.setPadding(Math.round(10 * mDensity), Math.round(6 * mDensity), Math.round(6 * mDensity), Math.round(6 * mDensity));

      GradientDrawable gd = new GradientDrawable();
      gd.setColor(Color.parseColor("#1E293B"));
      gd.setCornerRadius(8 * mDensity);
      gd.setStroke(Math.round(1.5f * mDensity), Color.parseColor("#F59E0B")); // Amber border
      chip.setBackground(gd);

      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
      lp.setMargins(0, 0, Math.round(8 * mDensity), 0);
      chip.setLayoutParams(lp);

      // Label with icon
      TextView labelView = new TextView(this);
      labelView.setText(CustomSymbolStore.getFriendlyActionLabel(code));
      labelView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
      labelView.setTypeface(null, Typeface.BOLD);
      labelView.setTextColor(Color.parseColor("#FDE68A")); // Soft gold
      labelView.setPadding(0, 0, Math.round(6 * mDensity), 0);

      // Click on chip body to assign
      chip.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          mEdtSymbolInput.setText(code);
          assignSymbol(code);
        }
      });

      // Discard button ✕
      TextView btnDiscard = new TextView(this);
      btnDiscard.setText("✕");
      btnDiscard.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
      btnDiscard.setTypeface(null, Typeface.BOLD);
      btnDiscard.setTextColor(Color.parseColor("#F87171"));
      btnDiscard.setPadding(Math.round(4 * mDensity), Math.round(2 * mDensity), Math.round(4 * mDensity), Math.round(2 * mDensity));
      btnDiscard.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          if (keyIndex >= 0 && keyIndex < mReservedKeys.size())
          {
            String removed = mReservedKeys.remove(keyIndex);
            CustomSymbolStore.saveReservedKeys(_prefs, mReservedKeys);
            renderReservedKeys();
            Toast.makeText(KeySymbolCustomizerActivity.this, "Discarded [" + CustomSymbolStore.getFriendlyActionLabel(removed) + "]", Toast.LENGTH_SHORT).show();
          }
        }
      });

      chip.addView(labelView);
      chip.addView(btnDiscard);
      mContainerReservedKeys.addView(chip);
    }
  }

  private void populateSpecialActionsPalette()
  {
    LinearLayout container = findViewById(R.id.palette_special_actions);
    if (container == null) return;
    container.removeAllViews();

    for (final CustomSymbolStore.SpecialAction sa : CustomSymbolStore.SPECIAL_ACTIONS)
    {
      TextView chip = new TextView(this);
      chip.setText(sa.label + "  " + sa.icon);
      chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
      chip.setTypeface(null, Typeface.BOLD);
      chip.setGravity(Gravity.CENTER);
      chip.setTextColor(Color.parseColor("#38BDF8"));
      chip.setPadding(Math.round(12 * mDensity), Math.round(7 * mDensity), Math.round(12 * mDensity), Math.round(7 * mDensity));

      GradientDrawable gd = new GradientDrawable();
      gd.setColor(Color.parseColor("#1E293B"));
      gd.setCornerRadius(6 * mDensity);
      gd.setStroke(Math.round(1 * mDensity), Color.parseColor("#3B82F6"));
      chip.setBackground(gd);

      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
      lp.setMargins(0, 0, Math.round(6 * mDensity), 0);
      chip.setLayoutParams(lp);

      chip.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          mEdtSymbolInput.setText(sa.code);
          assignSymbol(sa.code);
        }
      });

      container.addView(chip);
    }
  }

  private void populatePalette(int containerResId, String[] symbols)
  {
    LinearLayout container = findViewById(containerResId);
    if (container == null) return;
    container.removeAllViews();

    for (final String sym : symbols)
    {
      TextView chip = new TextView(this);
      chip.setText(sym);
      chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
      chip.setTypeface(null, Typeface.BOLD);
      chip.setGravity(Gravity.CENTER);
      chip.setTextColor(Color.WHITE);
      chip.setPadding(Math.round(13 * mDensity), Math.round(6 * mDensity), Math.round(13 * mDensity), Math.round(6 * mDensity));

      GradientDrawable gd = new GradientDrawable();
      gd.setColor(Color.parseColor("#374151"));
      gd.setCornerRadius(6 * mDensity);
      gd.setStroke(Math.round(1 * mDensity), Color.parseColor("#4B5563"));
      chip.setBackground(gd);

      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
      lp.setMargins(0, 0, Math.round(6 * mDensity), 0);
      chip.setLayoutParams(lp);

      chip.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          mEdtSymbolInput.setText(sym);
          assignSymbol(sym);
        }
      });

      container.addView(chip);
    }
  }

  private void updateCompassGrid()
  {
    String displayKey = mSelectedKey.equals("space") ? "Space" : mSelectedKey.toUpperCase();
    if (mTxtSelectedKeyBadge != null)
    {
      mTxtSelectedKeyBadge.setText("Selected Key: [ " + displayKey + " ]");
    }

    String posName = CustomSymbolStore.getPosName(mSelectedPos);
    String posIcon = CustomSymbolStore.getPosIcon(mSelectedPos);
    if (mTxtTargetIndicator != null)
    {
      mTxtTargetIndicator.setText("Target: Key [ " + displayKey + " ]  •  " + posName + " (" + posIcon + ")");
    }

    KeyboardData.Key baseKeyObj = findKeyInBaseLayout(mSelectedKey);

    for (int i = 0; i < 9; i++)
    {
      LinearLayout cell = mCells[i];
      TextView txt = mCellTexts[i];
      TextView status = mCellStatuses[i];
      if (cell == null || txt == null) continue;

      boolean isSelected = (i == mSelectedPos);
      GradientDrawable gd = new GradientDrawable();
      gd.setCornerRadius(8 * mDensity);

      if (isSelected)
      {
        gd.setColor(Color.parseColor("#1E3A8A"));
        gd.setStroke(Math.round(2 * mDensity), Color.parseColor("#60A5FA"));
      }
      else if (i == 0)
      {
        gd.setColor(Color.parseColor("#374151"));
        gd.setStroke(Math.round(1 * mDensity), Color.parseColor("#4B5563"));
      }
      else
      {
        gd.setColor(Color.parseColor("#1F2937"));
        gd.setStroke(Math.round(1 * mDensity), Color.parseColor("#374151"));
      }
      cell.setBackground(gd);

      // Find if this position has a custom mapping
      String mappedSym = getCustomMappingFor(mSelectedKey, i);

      if (mappedSym != null)
      {
        if (CustomSymbolStore.SYMBOL_EMPTY.equalsIgnoreCase(mappedSym) || "none".equalsIgnoreCase(mappedSym))
        {
          txt.setText("— (Cleared)");
          txt.setTextColor(Color.parseColor("#F59E0B")); // Amber
          txt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
          if (status != null)
          {
            status.setText("[Cleared]");
            status.setTextColor(Color.parseColor("#F59E0B"));
          }
        }
        else
        {
          txt.setText(CustomSymbolStore.getFriendlyActionLabel(mappedSym));
          txt.setTextColor(Color.parseColor("#50FA7B"));
          txt.setTextSize(TypedValue.COMPLEX_UNIT_SP, (mappedSym.length() > 3) ? 12 : 16);
          if (status != null)
          {
            status.setText("[Custom]");
            status.setTextColor(Color.parseColor("#50FA7B"));
          }
        }
      }
      else if (i == 0)
      {
        txt.setText(displayKey);
        txt.setTextColor(Color.parseColor("#60A5FA"));
        txt.setTextSize(TypedValue.COMPLEX_UNIT_SP, (displayKey.length() > 3) ? 13 : 18);
        if (status != null)
        {
          status.setText("[Main]");
          status.setTextColor(Color.parseColor("#60A5FA"));
        }
      }
      else if (baseKeyObj != null && baseKeyObj.keys != null && i < baseKeyObj.keys.length && baseKeyObj.keys[i] != null)
      {
        // Display default character or extra key for this corner!
        KeyValue kv = baseKeyObj.keys[i];
        String defStr = CustomSymbolStore.formatKeyValueDisplay(kv);
        txt.setText(defStr);
        txt.setTextColor(Color.parseColor("#93C5FD")); // Soft sky blue for default/extra keys
        txt.setTextSize(TypedValue.COMPLEX_UNIT_SP, (defStr.length() > 3) ? 12 : 16);
        if (status != null)
        {
          status.setText("[Default]");
          status.setTextColor(Color.parseColor("#60A5FA"));
        }
      }
      else
      {
        txt.setText("—");
        txt.setTextColor(Color.parseColor("#6B7280"));
        txt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        if (status != null)
        {
          status.setText("[Empty]");
          status.setTextColor(Color.parseColor("#6B7280"));
        }
      }
    }
    renderReservedKeys();
  }

  private void assignSymbol(String symbol)
  {
    CustomSymbolStore.Mapping newMapping = new CustomSymbolStore.Mapping(mSelectedKey, mSelectedPos, symbol);
    boolean replaced = false;
    for (int i = 0; i < mMappings.size(); i++)
    {
      CustomSymbolStore.Mapping m = mMappings.get(i);
      if (m.baseKey.equalsIgnoreCase(mSelectedKey) && m.pos == mSelectedPos)
      {
        mMappings.set(i, newMapping);
        replaced = true;
        break;
      }
    }
    if (!replaced)
    {
      mMappings.add(newMapping);
    }

    // Consume from reserved keys if present
    if (mReservedKeys.remove(symbol))
    {
      CustomSymbolStore.saveReservedKeys(_prefs, mReservedKeys);
      renderReservedKeys();
    }

    String label = CustomSymbolStore.getFriendlyActionLabel(symbol);
    Toast.makeText(this, "Set " + label + " to Key [" + mSelectedKey.toUpperCase() + "] (" + CustomSymbolStore.getPosIcon(mSelectedPos) + ")", Toast.LENGTH_SHORT).show();
    mActiveKeyboardData = LayoutModifier.apply_custom_symbols(mBaseKeyboardData, mMappings);
    renderLiveKeyboard();
    updateCompassGrid();
    buildActiveMappingsList();
  }

  private void clearSelectedSlot()
  {
    KeyboardData.Key baseKeyObj = findKeyInBaseLayout(mSelectedKey);
    boolean baseHasSymbol = (baseKeyObj != null && baseKeyObj.keys != null
        && mSelectedPos < baseKeyObj.keys.length && baseKeyObj.keys[mSelectedPos] != null);

    String existingCustom = getCustomMappingFor(mSelectedKey, mSelectedPos);
    String codeToReserve = null;

    if (existingCustom != null && !CustomSymbolStore.SYMBOL_EMPTY.equalsIgnoreCase(existingCustom) && !"none".equalsIgnoreCase(existingCustom))
    {
      codeToReserve = existingCustom;
    }
    else if (existingCustom == null && baseHasSymbol)
    {
      codeToReserve = CustomSymbolStore.getCanonicalCodeForKeyValue(baseKeyObj.keys[mSelectedPos]);
    }

    boolean removedExistingCustom = false;
    boolean wasAlreadyEmpty = false;

    for (int i = mMappings.size() - 1; i >= 0; i--)
    {
      CustomSymbolStore.Mapping m = mMappings.get(i);
      if (m.baseKey.equalsIgnoreCase(mSelectedKey) && m.pos == mSelectedPos)
      {
        if (CustomSymbolStore.SYMBOL_EMPTY.equalsIgnoreCase(m.symbol))
        {
          wasAlreadyEmpty = true;
        }
        mMappings.remove(i);
        removedExistingCustom = true;
      }
    }

    if (codeToReserve != null && !codeToReserve.isEmpty() && !CustomSymbolStore.SYMBOL_EMPTY.equalsIgnoreCase(codeToReserve))
    {
      mReservedKeys.add(codeToReserve);
      CustomSymbolStore.saveReservedKeys(_prefs, mReservedKeys);
      renderReservedKeys();
    }

    if (baseHasSymbol && !wasAlreadyEmpty)
    {
      mMappings.add(new CustomSymbolStore.Mapping(mSelectedKey, mSelectedPos, CustomSymbolStore.SYMBOL_EMPTY));
      String reservedMsg = (codeToReserve != null) ? " & reserved [" + CustomSymbolStore.getFriendlyActionLabel(codeToReserve) + "]" : "";
      Toast.makeText(this, "Cleared" + reservedMsg + " from Key [" + mSelectedKey.toUpperCase() + "]", Toast.LENGTH_SHORT).show();
    }
    else if (removedExistingCustom)
    {
      String reservedMsg = (codeToReserve != null) ? " & reserved [" + CustomSymbolStore.getFriendlyActionLabel(codeToReserve) + "]" : "";
      Toast.makeText(this, "Cleared" + reservedMsg + " on Key [" + mSelectedKey.toUpperCase() + "]", Toast.LENGTH_SHORT).show();
    }
    else
    {
      Toast.makeText(this, "Slot is already empty", Toast.LENGTH_SHORT).show();
    }

    mEdtSymbolInput.setText("");
    mActiveKeyboardData = LayoutModifier.apply_custom_symbols(mBaseKeyboardData, mMappings);
    renderLiveKeyboard();
    updateCompassGrid();
    buildActiveMappingsList();
  }

  private void buildActiveMappingsList()
  {
    if (mContainerActiveMappings == null) return;
    mContainerActiveMappings.removeAllViews();

    if (mMappings == null || mMappings.isEmpty())
    {
      TextView emptyTxt = new TextView(this);
      emptyTxt.setText("No custom key symbols assigned yet.\nTap any key and corner above to assign symbols or special actions!");
      emptyTxt.setTextColor(Color.parseColor("#94A3B8"));
      emptyTxt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
      emptyTxt.setGravity(Gravity.CENTER);
      emptyTxt.setPadding(0, Math.round(16 * mDensity), 0, Math.round(16 * mDensity));
      mContainerActiveMappings.addView(emptyTxt);
      return;
    }

    List<CustomSymbolStore.Mapping> sortedList = new ArrayList<CustomSymbolStore.Mapping>(mMappings);
    Collections.sort(sortedList, new Comparator<CustomSymbolStore.Mapping>()
    {
      @Override
      public int compare(CustomSymbolStore.Mapping a, CustomSymbolStore.Mapping b)
      {
        int cmp = a.baseKey.compareToIgnoreCase(b.baseKey);
        if (cmp != 0) return cmp;
        return Integer.compare(a.pos, b.pos);
      }
    });

    for (final CustomSymbolStore.Mapping m : sortedList)
    {
      LinearLayout row = new LinearLayout(this);
      row.setOrientation(LinearLayout.HORIZONTAL);
      row.setGravity(Gravity.CENTER_VERTICAL);
      row.setPadding(Math.round(12 * mDensity), Math.round(10 * mDensity), Math.round(12 * mDensity), Math.round(10 * mDensity));

      GradientDrawable gd = new GradientDrawable();
      gd.setColor(Color.parseColor("#1F2937"));
      gd.setCornerRadius(6 * mDensity);
      gd.setStroke(Math.round(1 * mDensity), Color.parseColor("#374151"));
      row.setBackground(gd);

      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
      lp.setMargins(0, 0, 0, Math.round(6 * mDensity));
      row.setLayoutParams(lp);

      TextView txtKey = new TextView(this);
      String displayKey = m.baseKey.equals("space") ? "Space" : m.baseKey.toUpperCase();
      txtKey.setText("Key [ " + displayKey + " ]");
      txtKey.setTextColor(Color.parseColor("#60A5FA"));
      txtKey.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
      txtKey.setTypeface(null, Typeface.BOLD);
      row.addView(txtKey);

      TextView txtArrow1 = new TextView(this);
      txtArrow1.setText(" • " + CustomSymbolStore.getPosName(m.pos) + " (" + CustomSymbolStore.getPosIcon(m.pos) + ") ➔ ");
      txtArrow1.setTextColor(Color.parseColor("#9CA3AF"));
      txtArrow1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
      row.addView(txtArrow1);

      TextView txtSymbol = new TextView(this);
      if (CustomSymbolStore.SYMBOL_EMPTY.equalsIgnoreCase(m.symbol) || "none".equalsIgnoreCase(m.symbol))
      {
        txtSymbol.setText("Cleared (Empty)");
        txtSymbol.setTextColor(Color.parseColor("#F59E0B")); // Amber
      }
      else
      {
        txtSymbol.setText(CustomSymbolStore.getFriendlyActionLabel(m.symbol));
        txtSymbol.setTextColor(Color.parseColor("#50FA7B"));
      }
      txtSymbol.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
      txtSymbol.setTypeface(null, Typeface.BOLD);
      LinearLayout.LayoutParams symLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
      txtSymbol.setLayoutParams(symLp);
      row.addView(txtSymbol);

      Button btnDel = new Button(this);
      btnDel.setText("✕");
      btnDel.setTextColor(Color.parseColor("#F87171"));
      btnDel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
      btnDel.setBackgroundColor(Color.TRANSPARENT);
      btnDel.setPadding(Math.round(8 * mDensity), 0, Math.round(8 * mDensity), 0);
      btnDel.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          mMappings.remove(m);
          mActiveKeyboardData = LayoutModifier.apply_custom_symbols(mBaseKeyboardData, mMappings);
          renderLiveKeyboard();
          updateCompassGrid();
          buildActiveMappingsList();
        }
      });
      row.addView(btnDel);

      mContainerActiveMappings.addView(row);
    }
  }

  private void showResetAllConfirmDialog()
  {
    new AlertDialog.Builder(this)
        .setTitle("Reset All Custom Symbols?")
        .setMessage("This will remove all custom symbol and special action mappings and restore keyboard defaults.")
        .setPositiveButton("Reset", new DialogInterface.OnClickListener()
        {
          @Override
          public void onClick(DialogInterface dialog, int which)
          {
            mMappings.clear();
            CustomSymbolStore.clearAll(_prefs);
            if (Config.globalConfig() != null)
            {
              Config.globalConfig().custom_symbol_mappings = new ArrayList<CustomSymbolStore.Mapping>();
            }
            mActiveKeyboardData = LayoutModifier.apply_custom_symbols(mBaseKeyboardData, mMappings);
            renderLiveKeyboard();
            updateCompassGrid();
            buildActiveMappingsList();
            Toast.makeText(KeySymbolCustomizerActivity.this, "All custom symbols reset to defaults", Toast.LENGTH_SHORT).show();
          }
        })
        .setNegativeButton("Cancel", null)
        .show();
  }

  private void saveAndApply()
  {
    CustomSymbolStore.saveMappings(_prefs, mMappings);

    if (Config.globalConfig() != null)
    {
      Config.globalConfig().custom_symbol_mappings = new ArrayList<CustomSymbolStore.Mapping>(mMappings);
    }

    Toast.makeText(this, "✓ Custom symbols & actions saved and applied to keyboard!", Toast.LENGTH_SHORT).show();
  }
}
