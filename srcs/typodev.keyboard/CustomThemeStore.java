package typodev.keyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class CustomThemeStore
{
  private static final String PREF_CUSTOM_ENABLED = "custom_theme_enabled";
  public static final String PREF_KEYBOARD_BG = "custom_theme_keyboard_bg";
  public static final String PREF_KEY_NORMAL = "custom_theme_key_normal";
  public static final String PREF_KEY_SPACE = "custom_theme_key_space";
  public static final String PREF_KEY_SHIFT = "custom_theme_key_shift";
  public static final String PREF_KEY_CTRL = "custom_theme_key_ctrl";
  public static final String PREF_KEY_ENTER = "custom_theme_key_enter";
  public static final String PREF_KEY_BACKSPACE = "custom_theme_key_backspace";
  public static final String PREF_LABEL_COLOR = "custom_theme_label_color";
  public static final String PREF_SUBLABEL_COLOR = "custom_theme_sublabel_color";
  public static final String PREF_SUGGESTION_BG = "custom_theme_suggestion_bg";
  public static final String PREF_SUGGESTION_CHIP_BG = "custom_theme_suggestion_chip_bg";
  public static final String PREF_SUGGESTION_TEXT_COLOR = "custom_theme_suggestion_text_color";
  public static final String PREF_BORDER_ENABLED = "custom_theme_border_enabled";
  public static final String PREF_BORDER_COLOR = "custom_theme_border_color";
  public static final String PREF_BORDER_WIDTH = "custom_theme_border_width";
  public static final String PREF_BORDER_RADIUS = "custom_theme_border_radius";
  public static final String PREF_BOTTOM_BORDER_ENABLED = "custom_theme_bottom_border_enabled";
  public static final String PREF_BOTTOM_BORDER_COLOR = "custom_theme_bottom_border_color";
  public static final String PREF_BOTTOM_BORDER_HEIGHT = "custom_theme_bottom_border_height";
  public static final String PREF_OUTER_CORNER_RADIUS = "custom_theme_outer_corner_radius";
  public static final String PREF_HAS_GRADIENT = "custom_theme_has_gradient";
  public static final String PREF_GRADIENT_START = "custom_theme_gradient_start";
  public static final String PREF_GRADIENT_END = "custom_theme_gradient_end";
  public static final String PREF_ACTION_LABEL_COLOR = "custom_theme_action_label_color";
  public static final String PREF_KEY_PRESSED_BG_COLOR = "custom_theme_key_pressed_bg_color";
  public static final String PREF_KEY_PRESSED_TEXT_COLOR = "custom_theme_key_pressed_text_color";
  public static final String PREF_POPUP_BG_COLOR = "custom_theme_popup_bg_color";
  public static final String PREF_POPUP_TEXT_COLOR = "custom_theme_popup_text_color";
  public static final String PREF_BOTTOM_BORDER_SHIFT = "custom_theme_bottom_border_shift";
  public static final String PREF_BOTTOM_BORDER_CTRL = "custom_theme_bottom_border_ctrl";
  public static final String PREF_BOTTOM_BORDER_BACKSPACE = "custom_theme_bottom_border_backspace";
  public static final String PREF_BOTTOM_BORDER_SPACE = "custom_theme_bottom_border_space";
  public static final String PREF_BOTTOM_BORDER_ENTER = "custom_theme_bottom_border_enter";
  public static final String PREF_KEY_SHIFT_TEXT_COLOR = "custom_theme_key_shift_text_color";
  public static final String PREF_KEY_CTRL_TEXT_COLOR = "custom_theme_key_ctrl_text_color";
  public static final String PREF_KEY_ENTER_TEXT_COLOR = "custom_theme_key_enter_text_color";
  public static final String PREF_KEY_BACKSPACE_TEXT_COLOR = "custom_theme_key_backspace_text_color";
  public static final String PREF_KEY_LOCKED_TEXT_COLOR = "custom_theme_key_locked_text_color";
  public static final String PREF_KEY_ACTIVATED_TEXT_COLOR = "custom_theme_key_activated_text_color";

  public static final float DEFAULT_OUTER_CORNER_RADIUS = 0.0f;

  // Dark theme defaults (matching TypoDev's primary dark keyboard)
  public static final int DEFAULT_KEYBOARD_BG = 0xFF1B1B1B;
  public static final int DEFAULT_KEY_NORMAL = 0xFF333333;
  public static final int DEFAULT_KEY_SPACE = 0xFF2B2B2B;
  public static final int DEFAULT_KEY_SHIFT = 0xFF262626;
  public static final int DEFAULT_KEY_CTRL = 0xFF262626;
  public static final int DEFAULT_KEY_ENTER = 0xFF262626;
  public static final int DEFAULT_KEY_BACKSPACE = 0xFF262626;
  public static final int DEFAULT_LABEL_COLOR = 0xFFFFFFFF;
  public static final int DEFAULT_ACTION_LABEL_COLOR = 0xFFFFFFFF;
  public static final int DEFAULT_LOCKED_TEXT_COLOR = 0xFF33CC33;
  public static final int DEFAULT_ACTIVATED_TEXT_COLOR = 0xFF0099FF;
  public static final int DEFAULT_KEY_PRESSED_BG_COLOR = 0xFF4A4A4A;
  public static final int DEFAULT_KEY_PRESSED_TEXT_COLOR = 0xFFFFFFFF;
  public static final int DEFAULT_POPUP_BG_COLOR = 0xFF333333;
  public static final int DEFAULT_POPUP_TEXT_COLOR = 0xFFFFFFFF;
  public static final boolean DEFAULT_BOTTOM_BORDER_SHIFT = true;
  public static final boolean DEFAULT_BOTTOM_BORDER_CTRL = true;
  public static final boolean DEFAULT_BOTTOM_BORDER_BACKSPACE = true;
  public static final boolean DEFAULT_BOTTOM_BORDER_SPACE = true;
  public static final boolean DEFAULT_BOTTOM_BORDER_ENTER = true;
  public static final int DEFAULT_SUBLABEL_COLOR = 0xFFCCCCCC;
  public static final int DEFAULT_SUGGESTION_BG = 0xFF1B1B1B;
  public static final int DEFAULT_SUGGESTION_CHIP_BG = 0xFF262626;
  public static final int DEFAULT_SUGGESTION_TEXT_COLOR = 0xFF3399FF;
  public static final boolean DEFAULT_BORDER_ENABLED = false;
  public static final int DEFAULT_BORDER_COLOR = 0xFF404040;
  public static final float DEFAULT_BORDER_WIDTH = 1.2f;
  public static final float DEFAULT_BORDER_RADIUS = 5.0f;
  public static final boolean DEFAULT_BOTTOM_BORDER_ENABLED = true;
  public static final int DEFAULT_BOTTOM_BORDER_COLOR = 0xFF404040;
  public static final float DEFAULT_BOTTOM_BORDER_HEIGHT = 1.2f;
  public static final boolean DEFAULT_HAS_GRADIENT = false;
  public static final int DEFAULT_GRADIENT_START = 0xFF8A2BE2;
  public static final int DEFAULT_GRADIENT_END = 0xFFAF6679;

  // Dracula theme defaults
  public static final int DRACULA_KEYBOARD_BG = 0xFF282A36;
  public static final int DRACULA_KEY_NORMAL = 0xFF343746;
  public static final int DRACULA_KEY_SPACE = 0xFF44475A; // Dracula Current Line
  public static final int DRACULA_KEY_SHIFT = 0xFFBD93F9; // Dracula Purple
  public static final int DRACULA_KEY_CTRL = 0xFF44475A; // Dracula Current Line
  public static final int DRACULA_KEY_ENTER = 0xFF50FA7B; // Dracula Green
  public static final int DRACULA_KEY_BACKSPACE = 0xFFFF5555; // Dracula Coral Red
  public static final int DRACULA_LABEL_COLOR = 0xFFF8F8F2; // Dracula Foreground White
  public static final int DRACULA_SUBLABEL_COLOR = 0xFF6272A4; // Dracula Slate
  public static final int DRACULA_SUGGESTION_BG = 0xFF21222C; // Dracula Deeper Dark
  public static final int DRACULA_SUGGESTION_CHIP_BG = 0xFF343746;
  public static final int DRACULA_SUGGESTION_TEXT_COLOR = 0xFF50FA7B;

  private static CustomThemeStore sInstance;
  private final Context _context;
  private final SharedPreferences _prefs;

  public static synchronized CustomThemeStore instance(Context context)
  {
    if (sInstance == null)
    {
      sInstance = new CustomThemeStore(context.getApplicationContext());
    }
    return sInstance;
  }

  private CustomThemeStore(Context context)
  {
    _context = context;
    // Theme rendering also runs before the user unlocks the device.
    _prefs = DirectBootAwarePreferences.get_shared_preferences(context);
  }

  private SharedPreferences editablePreferences()
  {
    return PreferenceManager.getDefaultSharedPreferences(_context);
  }

  private void synchronizePreferences()
  {
    DirectBootAwarePreferences.copy_preferences_to_protected_storage(
        _context, editablePreferences());
  }

  public boolean isCustomThemeActive(Config config)
  {
    String themeName = _prefs.getString("theme", "system");
    return "custom".equals(themeName);
  }

  public boolean isCustomThemeActive()
  {
    String themeName = _prefs.getString("theme", "system");
    return "custom".equals(themeName);
  }

  public void setCustomThemeActive(boolean active)
  {
    editablePreferences().edit()
        .putString("theme", active ? "custom" : "system")
        .putBoolean(PREF_CUSTOM_ENABLED, active)
        .apply();
    synchronizePreferences();
  }

  public void resetToDefaultTheme()
  {
    editablePreferences().edit()
        .putString("theme", "system")
        .putBoolean(PREF_CUSTOM_ENABLED, false)
        .remove(PREF_KEYBOARD_BG)
        .remove(PREF_KEY_NORMAL)
        .remove(PREF_KEY_SPACE)
        .remove(PREF_KEY_SHIFT)
        .remove(PREF_KEY_CTRL)
        .remove(PREF_KEY_ENTER)
        .remove(PREF_KEY_BACKSPACE)
        .remove(PREF_LABEL_COLOR)
        .remove(PREF_SUBLABEL_COLOR)
        .remove(PREF_SUGGESTION_BG)
        .remove(PREF_SUGGESTION_CHIP_BG)
        .remove(PREF_SUGGESTION_TEXT_COLOR)
        .remove(PREF_BORDER_ENABLED)
        .remove(PREF_BORDER_COLOR)
        .remove(PREF_BORDER_WIDTH)
        .remove(PREF_BORDER_RADIUS)
        .remove(PREF_BOTTOM_BORDER_ENABLED)
        .remove(PREF_BOTTOM_BORDER_COLOR)
        .remove(PREF_BOTTOM_BORDER_HEIGHT)
        .remove(PREF_OUTER_CORNER_RADIUS)
        .remove(PREF_HAS_GRADIENT)
        .remove(PREF_GRADIENT_START)
        .remove(PREF_GRADIENT_END)
        .remove(PREF_ACTION_LABEL_COLOR)
        .remove(PREF_KEY_PRESSED_BG_COLOR)
        .remove(PREF_KEY_PRESSED_TEXT_COLOR)
        .remove(PREF_POPUP_BG_COLOR)
        .remove(PREF_POPUP_TEXT_COLOR)
        .remove(PREF_BOTTOM_BORDER_SHIFT)
        .remove(PREF_BOTTOM_BORDER_CTRL)
        .remove(PREF_BOTTOM_BORDER_BACKSPACE)
        .remove(PREF_BOTTOM_BORDER_SPACE)
        .remove(PREF_BOTTOM_BORDER_ENTER)
        .remove(PREF_KEY_SHIFT_TEXT_COLOR)
        .remove(PREF_KEY_CTRL_TEXT_COLOR)
        .remove(PREF_KEY_ENTER_TEXT_COLOR)
        .remove(PREF_KEY_BACKSPACE_TEXT_COLOR)
        .remove(PREF_KEY_LOCKED_TEXT_COLOR)
        .remove(PREF_KEY_ACTIVATED_TEXT_COLOR)
        .apply();
    synchronizePreferences();
  }

  public boolean hasSavedCustomTheme()
  {
    return _prefs.contains(PREF_KEYBOARD_BG);
  }

  public int getKeyboardBg()
  {
    return _prefs.getInt(PREF_KEYBOARD_BG, DEFAULT_KEYBOARD_BG);
  }

  public int getKeyNormal()
  {
    return _prefs.getInt(PREF_KEY_NORMAL, DEFAULT_KEY_NORMAL);
  }

  public int getKeySpace()
  {
    return _prefs.getInt(PREF_KEY_SPACE, DEFAULT_KEY_SPACE);
  }

  public int getKeyShift()
  {
    return _prefs.getInt(PREF_KEY_SHIFT, DEFAULT_KEY_SHIFT);
  }

  public int getKeyCtrl()
  {
    return _prefs.getInt(PREF_KEY_CTRL, DEFAULT_KEY_CTRL);
  }

  public int getKeyEnter()
  {
    return _prefs.getInt(PREF_KEY_ENTER, DEFAULT_KEY_ENTER);
  }

  public int getKeyBackspace()
  {
    return _prefs.getInt(PREF_KEY_BACKSPACE, DEFAULT_KEY_BACKSPACE);
  }

  public int getLabelColor()
  {
    return _prefs.getInt(PREF_LABEL_COLOR, DEFAULT_LABEL_COLOR);
  }

  public int getSubLabelColor()
  {
    return _prefs.getInt(PREF_SUBLABEL_COLOR, DEFAULT_SUBLABEL_COLOR);
  }

  public int getActionLabelColor()
  {
    return _prefs.getInt(PREF_ACTION_LABEL_COLOR, getLabelColor());
  }

  public int getKeyPressedBgColor()
  {
    return _prefs.getInt(PREF_KEY_PRESSED_BG_COLOR, DEFAULT_KEY_PRESSED_BG_COLOR);
  }

  public int getKeyPressedTextColor()
  {
    return _prefs.getInt(PREF_KEY_PRESSED_TEXT_COLOR, getLabelColor());
  }

  public int getPopupBgColor()
  {
    return _prefs.getInt(PREF_POPUP_BG_COLOR, getKeyNormal());
  }

  public int getPopupTextColor()
  {
    return _prefs.getInt(PREF_POPUP_TEXT_COLOR, getLabelColor());
  }

  public int getShiftTextColor()
  {
    return _prefs.getInt(PREF_KEY_SHIFT_TEXT_COLOR, getActionLabelColor());
  }

  public int getCtrlTextColor()
  {
    return _prefs.getInt(PREF_KEY_CTRL_TEXT_COLOR, getActionLabelColor());
  }

  public int getEnterTextColor()
  {
    return _prefs.getInt(PREF_KEY_ENTER_TEXT_COLOR, getActionLabelColor());
  }

  public int getBackspaceTextColor()
  {
    return _prefs.getInt(PREF_KEY_BACKSPACE_TEXT_COLOR, getActionLabelColor());
  }

  public int getLockedTextColor()
  {
    return _prefs.getInt(PREF_KEY_LOCKED_TEXT_COLOR, DEFAULT_LOCKED_TEXT_COLOR);
  }

  public int getActivatedTextColor()
  {
    return _prefs.getInt(PREF_KEY_ACTIVATED_TEXT_COLOR, DEFAULT_ACTIVATED_TEXT_COLOR);
  }

  public static boolean isColorLight(int color)
  {
    double r = ((color >> 16) & 0xFF) / 255.0;
    double g = ((color >> 8) & 0xFF) / 255.0;
    double b = (color & 0xFF) / 255.0;
    double luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b;
    return luminance > 0.5;
  }

  public int getSuggestionBg()
  {
    int kbBg = getKeyboardBg();
    boolean light = isColorLight(kbBg);
    int defaultBg = light ? kbBg : DEFAULT_SUGGESTION_BG;
    int val = _prefs.getInt(PREF_SUGGESTION_BG, defaultBg);
    if (val == DEFAULT_SUGGESTION_BG && light)
    {
      return kbBg;
    }
    return val;
  }

  public int getSuggestionChipBg()
  {
    int kbBg = getKeyboardBg();
    boolean light = isColorLight(kbBg);
    int defaultChip = light ? (isColorLight(getKeyNormal()) ? getKeyNormal() : 0xFFFFFFFF) : DEFAULT_SUGGESTION_CHIP_BG;
    int val = _prefs.getInt(PREF_SUGGESTION_CHIP_BG, defaultChip);
    if (val == DEFAULT_SUGGESTION_CHIP_BG && light)
    {
      return defaultChip;
    }
    return val;
  }

  public int getSuggestionTextColor()
  {
    int kbBg = getKeyboardBg();
    boolean light = isColorLight(kbBg);
    int defaultText = light ? getLabelColor() : DEFAULT_SUGGESTION_TEXT_COLOR;
    int val = _prefs.getInt(PREF_SUGGESTION_TEXT_COLOR, defaultText);
    if (val == DEFAULT_SUGGESTION_TEXT_COLOR && light)
    {
      return defaultText;
    }
    return val;
  }

  public boolean isBorderEnabled()
  {
    return _prefs.getBoolean(PREF_BORDER_ENABLED, DEFAULT_BORDER_ENABLED);
  }

  public int getBorderColor()
  {
    return _prefs.getInt(PREF_BORDER_COLOR, DEFAULT_BORDER_COLOR);
  }

  public float getBorderWidthDp()
  {
    return _prefs.getFloat(PREF_BORDER_WIDTH, DEFAULT_BORDER_WIDTH);
  }

  public float getBorderRadiusDp()
  {
    return _prefs.getFloat(PREF_BORDER_RADIUS, DEFAULT_BORDER_RADIUS);
  }

  public boolean isBottomBorderEnabled()
  {
    return _prefs.getBoolean(PREF_BOTTOM_BORDER_ENABLED, DEFAULT_BOTTOM_BORDER_ENABLED);
  }

  public boolean isBottomBorderShift()
  {
    return _prefs.getBoolean(PREF_BOTTOM_BORDER_SHIFT, DEFAULT_BOTTOM_BORDER_SHIFT);
  }

  public boolean isBottomBorderCtrl()
  {
    return _prefs.getBoolean(PREF_BOTTOM_BORDER_CTRL, DEFAULT_BOTTOM_BORDER_CTRL);
  }

  public boolean isBottomBorderBackspace()
  {
    return _prefs.getBoolean(PREF_BOTTOM_BORDER_BACKSPACE, DEFAULT_BOTTOM_BORDER_BACKSPACE);
  }

  public boolean isBottomBorderSpace()
  {
    return _prefs.getBoolean(PREF_BOTTOM_BORDER_SPACE, DEFAULT_BOTTOM_BORDER_SPACE);
  }

  public boolean isBottomBorderEnter()
  {
    return _prefs.getBoolean(PREF_BOTTOM_BORDER_ENTER, DEFAULT_BOTTOM_BORDER_ENTER);
  }

  public int getBottomBorderColor()
  {
    return _prefs.getInt(PREF_BOTTOM_BORDER_COLOR, DEFAULT_BOTTOM_BORDER_COLOR);
  }

  public float getBottomBorderHeightDp()
  {
    return _prefs.getFloat(PREF_BOTTOM_BORDER_HEIGHT, DEFAULT_BOTTOM_BORDER_HEIGHT);
  }

  public float getOuterCornerRadiusDp()
  {
    return _prefs.getFloat(PREF_OUTER_CORNER_RADIUS, DEFAULT_OUTER_CORNER_RADIUS);
  }

  public boolean hasGradient()
  {
    return _prefs.getBoolean(PREF_HAS_GRADIENT, DEFAULT_HAS_GRADIENT);
  }

  public int getGradientStart()
  {
    return _prefs.getInt(PREF_GRADIENT_START, DEFAULT_GRADIENT_START);
  }

  public int getGradientEnd()
  {
    return _prefs.getInt(PREF_GRADIENT_END, DEFAULT_GRADIENT_END);
  }

  public void saveColors(int keyboardBg, int keyNormal, int keySpace, int keyShift, int keyCtrl,
                         int keyEnter, int keyBackspace, int labelColor, int subLabelColor,
                         int suggestionBg, int suggestionChipBg, int suggestionTextColor)
  {
    saveColors(keyboardBg, keyNormal, keySpace, keyShift, keyCtrl, keyEnter, keyBackspace,
        labelColor, subLabelColor, suggestionBg, suggestionChipBg, suggestionTextColor,
        isBorderEnabled(), getBorderColor(), getBorderWidthDp(), getBorderRadiusDp(),
        isBottomBorderEnabled(), getBottomBorderColor(), getBottomBorderHeightDp(),
        hasGradient(), getGradientStart(), getGradientEnd(), getOuterCornerRadiusDp());
  }

  public void saveColors(int keyboardBg, int keyNormal, int keySpace, int keyShift, int keyCtrl,
                         int keyEnter, int keyBackspace, int labelColor, int subLabelColor,
                         int suggestionBg, int suggestionChipBg, int suggestionTextColor,
                         boolean borderEnabled, int borderColor, float borderWidthDp, float borderRadiusDp)
  {
    saveColors(keyboardBg, keyNormal, keySpace, keyShift, keyCtrl, keyEnter, keyBackspace,
        labelColor, subLabelColor, suggestionBg, suggestionChipBg, suggestionTextColor,
        borderEnabled, borderColor, borderWidthDp, borderRadiusDp,
        isBottomBorderEnabled(), getBottomBorderColor(), getBottomBorderHeightDp(),
        hasGradient(), getGradientStart(), getGradientEnd(), getOuterCornerRadiusDp());
  }

  public void saveColors(int keyboardBg, int keyNormal, int keySpace, int keyShift, int keyCtrl,
                         int keyEnter, int keyBackspace, int labelColor, int subLabelColor,
                         int suggestionBg, int suggestionChipBg, int suggestionTextColor,
                         boolean borderEnabled, int borderColor, float borderWidthDp, float borderRadiusDp,
                         boolean hasGradient, int gradientStart, int gradientEnd)
  {
    saveColors(keyboardBg, keyNormal, keySpace, keyShift, keyCtrl, keyEnter, keyBackspace,
        labelColor, subLabelColor, suggestionBg, suggestionChipBg, suggestionTextColor,
        borderEnabled, borderColor, borderWidthDp, borderRadiusDp,
        isBottomBorderEnabled(), getBottomBorderColor(), getBottomBorderHeightDp(),
        hasGradient(), gradientStart, gradientEnd, getOuterCornerRadiusDp());
  }

  public void saveColors(int keyboardBg, int keyNormal, int keySpace, int keyShift, int keyCtrl,
                         int keyEnter, int keyBackspace, int labelColor, int subLabelColor,
                         int suggestionBg, int suggestionChipBg, int suggestionTextColor,
                         boolean borderEnabled, int borderColor, float borderWidthDp, float borderRadiusDp,
                         boolean bottomBorderEnabled, int bottomBorderColor, float bottomBorderHeightDp,
                         boolean hasGradient, int gradientStart, int gradientEnd)
  {
    saveColors(keyboardBg, keyNormal, keySpace, keyShift, keyCtrl, keyEnter, keyBackspace,
        labelColor, subLabelColor, suggestionBg, suggestionChipBg, suggestionTextColor,
        borderEnabled, borderColor, borderWidthDp, borderRadiusDp,
        bottomBorderEnabled, bottomBorderColor, bottomBorderHeightDp,
        hasGradient, gradientStart, gradientEnd, getOuterCornerRadiusDp());
  }

  public void saveColors(int keyboardBg, int keyNormal, int keySpace, int keyShift, int keyCtrl,
                         int keyEnter, int keyBackspace, int labelColor, int subLabelColor,
                         int suggestionBg, int suggestionChipBg, int suggestionTextColor,
                         boolean borderEnabled, int borderColor, float borderWidthDp, float borderRadiusDp,
                         boolean bottomBorderEnabled, int bottomBorderColor, float bottomBorderHeightDp,
                         boolean hasGradient, int gradientStart, int gradientEnd, float outerCornerRadiusDp)
  {
    saveColors(keyboardBg, keyNormal, keySpace, keyShift, keyCtrl,
        keyEnter, keyBackspace, labelColor, subLabelColor,
        suggestionBg, suggestionChipBg, suggestionTextColor,
        borderEnabled, borderColor, borderWidthDp, borderRadiusDp,
        bottomBorderEnabled, bottomBorderColor, bottomBorderHeightDp,
        hasGradient, gradientStart, gradientEnd, outerCornerRadiusDp,
        getActionLabelColor(), getKeyPressedBgColor(), getKeyPressedTextColor(),
        getPopupBgColor(), getPopupTextColor(),
        isBottomBorderShift(), isBottomBorderCtrl(), isBottomBorderBackspace(),
        isBottomBorderSpace(), isBottomBorderEnter());
  }

  public void saveColors(int keyboardBg, int keyNormal, int keySpace, int keyShift, int keyCtrl,
                         int keyEnter, int keyBackspace, int labelColor, int subLabelColor,
                         int suggestionBg, int suggestionChipBg, int suggestionTextColor,
                         boolean borderEnabled, int borderColor, float borderWidthDp, float borderRadiusDp,
                         boolean bottomBorderEnabled, int bottomBorderColor, float bottomBorderHeightDp,
                         boolean hasGradient, int gradientStart, int gradientEnd, float outerCornerRadiusDp,
                         int actionLabelColor, int keyPressedBgColor, int keyPressedTextColor,
                         int popupBgColor, int popupTextColor,
                         boolean bottomBorderShift, boolean bottomBorderCtrl, boolean bottomBorderBackspace,
                         boolean bottomBorderSpace, boolean bottomBorderEnter)
  {
    saveColors(keyboardBg, keyNormal, keySpace, keyShift, keyCtrl,
        keyEnter, keyBackspace, labelColor, subLabelColor,
        suggestionBg, suggestionChipBg, suggestionTextColor,
        borderEnabled, borderColor, borderWidthDp, borderRadiusDp,
        bottomBorderEnabled, bottomBorderColor, bottomBorderHeightDp,
        hasGradient, gradientStart, gradientEnd, outerCornerRadiusDp,
        actionLabelColor, keyPressedBgColor, keyPressedTextColor,
        popupBgColor, popupTextColor,
        bottomBorderShift, bottomBorderCtrl, bottomBorderBackspace,
        bottomBorderSpace, bottomBorderEnter,
        getShiftTextColor(), getCtrlTextColor(), getEnterTextColor(), getBackspaceTextColor());
  }

  public void saveColors(int keyboardBg, int keyNormal, int keySpace, int keyShift, int keyCtrl,
                         int keyEnter, int keyBackspace, int labelColor, int subLabelColor,
                         int suggestionBg, int suggestionChipBg, int suggestionTextColor,
                         boolean borderEnabled, int borderColor, float borderWidthDp, float borderRadiusDp,
                         boolean bottomBorderEnabled, int bottomBorderColor, float bottomBorderHeightDp,
                         boolean hasGradient, int gradientStart, int gradientEnd, float outerCornerRadiusDp,
                         int actionLabelColor, int keyPressedBgColor, int keyPressedTextColor,
                         int popupBgColor, int popupTextColor,
                         boolean bottomBorderShift, boolean bottomBorderCtrl, boolean bottomBorderBackspace,
                         boolean bottomBorderSpace, boolean bottomBorderEnter,
                         int shiftTextColor, int ctrlTextColor, int enterTextColor, int backspaceTextColor)
  {
    saveColors(keyboardBg, keyNormal, keySpace, keyShift, keyCtrl,
        keyEnter, keyBackspace, labelColor, subLabelColor,
        suggestionBg, suggestionChipBg, suggestionTextColor,
        borderEnabled, borderColor, borderWidthDp, borderRadiusDp,
        bottomBorderEnabled, bottomBorderColor, bottomBorderHeightDp,
        hasGradient, gradientStart, gradientEnd, outerCornerRadiusDp,
        actionLabelColor, keyPressedBgColor, keyPressedTextColor,
        popupBgColor, popupTextColor,
        bottomBorderShift, bottomBorderCtrl, bottomBorderBackspace,
        bottomBorderSpace, bottomBorderEnter,
        shiftTextColor, ctrlTextColor, enterTextColor, backspaceTextColor,
        getLockedTextColor(), getActivatedTextColor());
  }

  public void saveColors(int keyboardBg, int keyNormal, int keySpace, int keyShift, int keyCtrl,
                         int keyEnter, int keyBackspace, int labelColor, int subLabelColor,
                         int suggestionBg, int suggestionChipBg, int suggestionTextColor,
                         boolean borderEnabled, int borderColor, float borderWidthDp, float borderRadiusDp,
                         boolean bottomBorderEnabled, int bottomBorderColor, float bottomBorderHeightDp,
                         boolean hasGradient, int gradientStart, int gradientEnd, float outerCornerRadiusDp,
                         int actionLabelColor, int keyPressedBgColor, int keyPressedTextColor,
                         int popupBgColor, int popupTextColor,
                         boolean bottomBorderShift, boolean bottomBorderCtrl, boolean bottomBorderBackspace,
                         boolean bottomBorderSpace, boolean bottomBorderEnter,
                         int shiftTextColor, int ctrlTextColor, int enterTextColor, int backspaceTextColor,
                         int lockedTextColor, int activatedTextColor)
  {
    editablePreferences().edit()
        .putString("theme", "custom")
        .putBoolean(PREF_CUSTOM_ENABLED, true)
        .putInt(PREF_KEYBOARD_BG, keyboardBg)
        .putInt(PREF_KEY_NORMAL, keyNormal)
        .putInt(PREF_KEY_SPACE, keySpace)
        .putInt(PREF_KEY_SHIFT, keyShift)
        .putInt(PREF_KEY_CTRL, keyCtrl)
        .putInt(PREF_KEY_ENTER, keyEnter)
        .putInt(PREF_KEY_BACKSPACE, keyBackspace)
        .putInt(PREF_LABEL_COLOR, labelColor)
        .putInt(PREF_SUBLABEL_COLOR, subLabelColor)
        .putInt(PREF_SUGGESTION_BG, suggestionBg)
        .putInt(PREF_SUGGESTION_CHIP_BG, suggestionChipBg)
        .putInt(PREF_SUGGESTION_TEXT_COLOR, suggestionTextColor)
        .putBoolean(PREF_BORDER_ENABLED, borderEnabled)
        .putInt(PREF_BORDER_COLOR, borderColor)
        .putFloat(PREF_BORDER_WIDTH, borderWidthDp)
        .putFloat(PREF_BORDER_RADIUS, borderRadiusDp)
        .putFloat(PREF_OUTER_CORNER_RADIUS, outerCornerRadiusDp)
        .putBoolean(PREF_BOTTOM_BORDER_ENABLED, bottomBorderEnabled)
        .putInt(PREF_BOTTOM_BORDER_COLOR, bottomBorderColor)
        .putFloat(PREF_BOTTOM_BORDER_HEIGHT, bottomBorderHeightDp)
        .putBoolean("border_config", borderEnabled)
        .putFloat("custom_border_line_width", borderWidthDp)
        .putInt("custom_border_radius", (int)(borderRadiusDp * 2.5f))
        .putBoolean(PREF_HAS_GRADIENT, hasGradient)
        .putInt(PREF_GRADIENT_START, gradientStart)
        .putInt(PREF_GRADIENT_END, gradientEnd)
        .putInt(PREF_ACTION_LABEL_COLOR, actionLabelColor)
        .putInt(PREF_KEY_PRESSED_BG_COLOR, keyPressedBgColor)
        .putInt(PREF_KEY_PRESSED_TEXT_COLOR, keyPressedTextColor)
        .putInt(PREF_POPUP_BG_COLOR, popupBgColor)
        .putInt(PREF_POPUP_TEXT_COLOR, popupTextColor)
        .putBoolean(PREF_BOTTOM_BORDER_SHIFT, bottomBorderShift)
        .putBoolean(PREF_BOTTOM_BORDER_CTRL, bottomBorderCtrl)
        .putBoolean(PREF_BOTTOM_BORDER_BACKSPACE, bottomBorderBackspace)
        .putBoolean(PREF_BOTTOM_BORDER_SPACE, bottomBorderSpace)
        .putBoolean(PREF_BOTTOM_BORDER_ENTER, bottomBorderEnter)
        .putInt(PREF_KEY_SHIFT_TEXT_COLOR, shiftTextColor)
        .putInt(PREF_KEY_CTRL_TEXT_COLOR, ctrlTextColor)
        .putInt(PREF_KEY_ENTER_TEXT_COLOR, enterTextColor)
        .putInt(PREF_KEY_BACKSPACE_TEXT_COLOR, backspaceTextColor)
        .putInt(PREF_KEY_LOCKED_TEXT_COLOR, lockedTextColor)
        .putInt(PREF_KEY_ACTIVATED_TEXT_COLOR, activatedTextColor)
        .apply();
    synchronizePreferences();
  }

  public void saveNamedTheme(String name, Palette palette)
  {
    if (name == null || name.trim().isEmpty() || palette == null) return;
    List<Palette> current = getSavedThemes();
    boolean found = false;
    for (int i = 0; i < current.size(); i++)
    {
      if (current.get(i).name.equalsIgnoreCase(name.trim()))
      {
        palette.name = name.trim();
        current.set(i, palette);
        found = true;
        break;
      }
    }
    if (!found)
    {
      palette.name = name.trim();
      current.add(palette);
    }
    persistSavedThemes(current);
  }

  public void deleteSavedTheme(String name)
  {
    if (name == null) return;
    List<Palette> current = getSavedThemes();
    for (int i = 0; i < current.size(); i++)
    {
      if (current.get(i).name.equalsIgnoreCase(name.trim()))
      {
        current.remove(i);
        break;
      }
    }
    persistSavedThemes(current);
  }

  public List<Palette> getSavedThemes()
  {
    List<Palette> list = new ArrayList<>();
    String jsonStr = _prefs.getString("custom_theme_saved_palettes_json", "");
    if (jsonStr == null || jsonStr.trim().isEmpty())
      return list;
    try
    {
      JSONArray arr = new JSONArray(jsonStr);
      for (int i = 0; i < arr.length(); i++)
      {
        JSONObject obj = arr.getJSONObject(i);
        int normal = obj.getInt("keyNormal");
        int space = obj.optInt("keySpace", normal);
        boolean hasBorder = obj.optBoolean("hasBorder", false);
        int borderColor = obj.optInt("borderColor", 0xFF4B5563);
        float borderWidth = (float)obj.optDouble("borderWidth", 1.0);
        float borderRadius = (float)obj.optDouble("borderRadius", 5.0);
        boolean hasBottomBorder = obj.optBoolean("hasBottomBorder", true);
        int bottomBorderColor = obj.optInt("bottomBorderColor", 0xFF404040);
        float bottomBorderHeight = (float)obj.optDouble("bottomBorderHeight", 1.2);
        boolean hasGrad = obj.optBoolean("hasGradient", false);
        int gradStart = obj.optInt("gradientStart", DEFAULT_GRADIENT_START);
        int gradEnd = obj.optInt("gradientEnd", DEFAULT_GRADIENT_END);
        float outerCornerRadius = (float)obj.optDouble("outerCornerRadius", 0.0);
        Palette pal = new Palette(
            obj.getString("name"),
            obj.getInt("kbBg"),
            normal,
            space,
            obj.getInt("keyShift"),
            obj.getInt("keyCtrl"),
            obj.getInt("keyEnter"),
            obj.getInt("keyBackspace"),
            obj.getInt("label"),
            obj.getInt("sublabel"),
            obj.getInt("suggBg"),
            obj.getInt("suggChip"),
            obj.getInt("suggText"),
            hasBorder,
            borderColor,
            borderWidth,
            borderRadius,
            hasBottomBorder,
            bottomBorderColor,
            bottomBorderHeight,
            hasGrad,
            gradStart,
            gradEnd,
            outerCornerRadius
        );
        pal.actionLabelColor = obj.optInt("actionLabel", pal.labelColor);
        pal.keyPressedBgColor = obj.optInt("keyPressedBg", DEFAULT_KEY_PRESSED_BG_COLOR);
        pal.keyPressedTextColor = obj.optInt("keyPressedText", pal.labelColor);
        pal.popupBgColor = obj.optInt("popupBg", pal.keyNormal);
        pal.popupTextColor = obj.optInt("popupText", pal.labelColor);
        pal.bottomBorderShift = obj.optBoolean("bottomBorderShift", true);
        pal.bottomBorderCtrl = obj.optBoolean("bottomBorderCtrl", true);
        pal.bottomBorderBackspace = obj.optBoolean("bottomBorderBackspace", true);
        pal.bottomBorderSpace = obj.optBoolean("bottomBorderSpace", true);
        pal.bottomBorderEnter = obj.optBoolean("bottomBorderEnter", true);
        pal.shiftTextColor = obj.optInt("shiftText", pal.actionLabelColor);
        pal.ctrlTextColor = obj.optInt("ctrlText", pal.actionLabelColor);
        pal.enterTextColor = obj.optInt("enterText", pal.actionLabelColor);
        pal.backspaceTextColor = obj.optInt("backspaceText", pal.actionLabelColor);
        pal.lockedTextColor = obj.optInt("lockedText", DEFAULT_LOCKED_TEXT_COLOR);
        pal.activatedTextColor = obj.optInt("activatedText", DEFAULT_ACTIVATED_TEXT_COLOR);
        list.add(pal);
      }
    }
    catch (Exception ignored) {}
    return list;
  }

  private void persistSavedThemes(List<Palette> list)
  {
    try
    {
      JSONArray arr = new JSONArray();
      for (Palette p : list)
      {
        JSONObject obj = new JSONObject();
        obj.put("name", p.name);
        obj.put("kbBg", p.keyboardBg);
        obj.put("keyNormal", p.keyNormal);
        obj.put("keySpace", p.keySpace);
        obj.put("keyShift", p.keyShift);
        obj.put("keyCtrl", p.keyCtrl);
        obj.put("keyEnter", p.keyEnter);
        obj.put("keyBackspace", p.keyBackspace);
        obj.put("label", p.labelColor);
        obj.put("sublabel", p.subLabelColor);
        obj.put("suggBg", p.suggestionBg);
        obj.put("suggChip", p.suggestionChipBg);
        obj.put("suggText", p.suggestionTextColor);
        obj.put("hasBorder", p.hasBorder);
        obj.put("borderColor", p.borderColor);
        obj.put("borderWidth", p.borderWidthDp);
        obj.put("borderRadius", p.borderRadiusDp);
        obj.put("outerCornerRadius", p.outerCornerRadiusDp);
        obj.put("hasBottomBorder", p.hasBottomBorder);
        obj.put("bottomBorderColor", p.bottomBorderColor);
        obj.put("bottomBorderHeight", p.bottomBorderHeightDp);
        obj.put("hasGradient", p.hasGradient);
        obj.put("gradientStart", p.gradientStart);
        obj.put("gradientEnd", p.gradientEnd);
        obj.put("actionLabel", p.actionLabelColor);
        obj.put("keyPressedBg", p.keyPressedBgColor);
        obj.put("keyPressedText", p.keyPressedTextColor);
        obj.put("popupBg", p.popupBgColor);
        obj.put("popupText", p.popupTextColor);
        obj.put("bottomBorderShift", p.bottomBorderShift);
        obj.put("bottomBorderCtrl", p.bottomBorderCtrl);
        obj.put("bottomBorderBackspace", p.bottomBorderBackspace);
        obj.put("bottomBorderSpace", p.bottomBorderSpace);
        obj.put("bottomBorderEnter", p.bottomBorderEnter);
        obj.put("shiftText", p.shiftTextColor);
        obj.put("ctrlText", p.ctrlTextColor);
        obj.put("enterText", p.enterTextColor);
        obj.put("backspaceText", p.backspaceTextColor);
        obj.put("lockedText", p.lockedTextColor);
        obj.put("activatedText", p.activatedTextColor);
        arr.put(obj);
      }
      editablePreferences().edit().putString("custom_theme_saved_palettes_json", arr.toString()).apply();
      synchronizePreferences();
    }
    catch (Exception ignored) {}
  }

  public static class Palette
  {
    public String name;
    public int keyboardBg;
    public int keyNormal;
    public int keySpace;
    public int keyShift;
    public int keyCtrl;
    public int keyEnter;
    public int keyBackspace;
    public int labelColor;
    public int subLabelColor;
    public int suggestionBg;
    public int suggestionChipBg;
    public int suggestionTextColor;
    public boolean hasBorder;
    public int borderColor;
    public float borderWidthDp;
    public float borderRadiusDp;
    public float outerCornerRadiusDp = 0.0f;
    public boolean hasBottomBorder = true;
    public int bottomBorderColor = 0xFF404040;
    public float bottomBorderHeightDp = 1.2f;
    public boolean hasGradient;
    public int gradientStart;
    public int gradientEnd;
    public int actionLabelColor = DEFAULT_ACTION_LABEL_COLOR;
    public int shiftTextColor = DEFAULT_ACTION_LABEL_COLOR;
    public int ctrlTextColor = DEFAULT_ACTION_LABEL_COLOR;
    public int enterTextColor = DEFAULT_ACTION_LABEL_COLOR;
    public int backspaceTextColor = DEFAULT_ACTION_LABEL_COLOR;
    public int lockedTextColor = DEFAULT_LOCKED_TEXT_COLOR;
    public int activatedTextColor = DEFAULT_ACTIVATED_TEXT_COLOR;
    public int keyPressedBgColor = DEFAULT_KEY_PRESSED_BG_COLOR;
    public int keyPressedTextColor = DEFAULT_KEY_PRESSED_TEXT_COLOR;
    public int popupBgColor = DEFAULT_POPUP_BG_COLOR;
    public int popupTextColor = DEFAULT_POPUP_TEXT_COLOR;
    public boolean bottomBorderShift = true;
    public boolean bottomBorderCtrl = true;
    public boolean bottomBorderBackspace = true;
    public boolean bottomBorderSpace = true;
    public boolean bottomBorderEnter = true;

    public Palette(String name, int kbBg, int normal, int space, int shift, int ctrl, int enter, int backspace,
                   int label, int sublabel, int suggBg, int suggChip, int suggText,
                   boolean hasBorder, int borderColor, float borderWidthDp, float borderRadiusDp,
                   boolean hasBottomBorder, int bottomBorderColor, float bottomBorderHeightDp,
                   boolean hasGradient, int gradientStart, int gradientEnd, float outerCornerRadiusDp)
    {
      this(name, kbBg, normal, space, shift, ctrl, enter, backspace, label, sublabel, suggBg, suggChip, suggText,
           hasBorder, borderColor, borderWidthDp, borderRadiusDp, hasBottomBorder, bottomBorderColor, bottomBorderHeightDp,
           hasGradient, gradientStart, gradientEnd);
      this.outerCornerRadiusDp = outerCornerRadiusDp;
    }

    public Palette(String name, int kbBg, int normal, int space, int shift, int ctrl, int enter, int backspace,
                   int label, int sublabel, int suggBg, int suggChip, int suggText,
                   boolean hasBorder, int borderColor, float borderWidthDp, float borderRadiusDp,
                   boolean hasBottomBorder, int bottomBorderColor, float bottomBorderHeightDp,
                   boolean hasGradient, int gradientStart, int gradientEnd)
    {
      this.name = name;
      this.keyboardBg = kbBg;
      this.keyNormal = normal;
      this.keySpace = space;
      this.keyShift = shift;
      this.keyCtrl = ctrl;
      this.keyEnter = enter;
      this.keyBackspace = backspace;
      this.labelColor = label;
      this.subLabelColor = sublabel;
      this.suggestionBg = suggBg;
      this.suggestionChipBg = suggChip;
      this.suggestionTextColor = suggText;
      this.hasBorder = hasBorder;
      this.borderColor = borderColor;
      this.borderWidthDp = borderWidthDp;
      this.borderRadiusDp = borderRadiusDp;
      this.hasBottomBorder = hasBottomBorder;
      this.bottomBorderColor = bottomBorderColor;
      this.bottomBorderHeightDp = bottomBorderHeightDp;
      this.hasGradient = hasGradient;
      this.gradientStart = gradientStart;
      this.gradientEnd = gradientEnd;
      this.actionLabelColor = label;
      this.shiftTextColor = label;
      this.ctrlTextColor = label;
      this.enterTextColor = label;
      this.backspaceTextColor = label;
      this.lockedTextColor = DEFAULT_LOCKED_TEXT_COLOR;
      this.activatedTextColor = DEFAULT_ACTIVATED_TEXT_COLOR;
      this.keyPressedBgColor = (shift != normal) ? shift : DEFAULT_KEY_PRESSED_BG_COLOR;
      this.keyPressedTextColor = label;
      this.popupBgColor = normal;
      this.popupTextColor = label;
    }

    public Palette(String name, int kbBg, int normal, int space, int shift, int ctrl, int enter, int backspace,
                   int label, int sublabel, int suggBg, int suggChip, int suggText,
                   boolean hasBorder, int borderColor, float borderWidthDp, float borderRadiusDp,
                   boolean hasGradient, int gradientStart, int gradientEnd)
    {
      this(name, kbBg, normal, space, shift, ctrl, enter, backspace, label, sublabel, suggBg, suggChip, suggText,
           hasBorder, borderColor, borderWidthDp, borderRadiusDp, true, 0xFF404040, 1.2f, hasGradient, gradientStart, gradientEnd);
    }

    public Palette(String name, int kbBg, int normal, int space, int shift, int ctrl, int enter, int backspace,
                   int label, int sublabel, int suggBg, int suggChip, int suggText,
                   boolean hasBorder, int borderColor, float borderWidthDp, float borderRadiusDp)
    {
      this(name, kbBg, normal, space, shift, ctrl, enter, backspace, label, sublabel, suggBg, suggChip, suggText,
           hasBorder, borderColor, borderWidthDp, borderRadiusDp, true, 0xFF404040, 1.2f, false, 0, 0);
    }

    public Palette(String name, int kbBg, int normal, int space, int shift, int ctrl, int enter, int backspace,
                   int label, int sublabel, int suggBg, int suggChip, int suggText)
    {
      this(name, kbBg, normal, space, shift, ctrl, enter, backspace, label, sublabel, suggBg, suggChip, suggText,
           false, 0xFF4B5563, 1.0f, 5.0f, true, 0xFF404040, 1.2f, false, 0, 0);
    }
  }

  public static Palette getSystemPreset(Context context)
  {
    boolean isNight = true;
    if (context != null)
    {
      int nightFlags = context.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
      isNight = nightFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }
    if (isNight)
    {
      return new Palette(
          "System settings",
          0xFF1B1B1B, // kbBg
          0xFF333333, // normal
          0xFF2B2B2B, // space
          0xFF262626, // shift (exact Dark action key)
          0xFF262626, // ctrl
          0xFF262626, // enter
          0xFF262626, // backspace
          0xFFFFFFFF, // label
          0xFFCCCCCC, // sublabel
          0xFF1B1B1B, // suggBg
          0xFF262626, // suggChip
          0xFF3399FF, // suggText
          false,
          0xFF404040,
          1.2f,
          5.0f
      );
    }
    else
    {
      return new Palette(
          "System settings",
          0xFFE3E3E3, // kbBg
          0xFFCCCCCC, // normal
          0xFFDEDEDE, // space
          0xFFD9D9D9, // shift (exact Light action key)
          0xFFD9D9D9, // ctrl
          0xFFD9D9D9, // enter
          0xFFD9D9D9, // backspace
          0xFF000000, // label
          0xFF333333, // sublabel
          0xFFE3E3E3, // suggBg
          0xFFDEDEDE, // suggChip
          0xFF0066CC, // suggText
          false,
          0xFFAAAAAA,
          0.8f,
          5.0f
      );
    }
  }

  public static Palette getDarkPreset()
  {
    return new Palette(
        "Dark",
        0xFF1B1B1B,
        0xFF333333,
        0xFF2B2B2B,
        0xFF262626,
        0xFF262626,
        0xFF262626,
        0xFF262626,
        0xFFFFFFFF,
        0xFFCCCCCC,
        0xFF1B1B1B,
        0xFF262626,
        0xFF3399FF,
        false,
        0xFF404040,
        1.2f,
        5.0f
    );
  }

  public static Palette getLightPreset()
  {
    return new Palette(
        "Light",
        0xFFE3E3E3,
        0xFFCCCCCC,
        0xFFDEDEDE,
        0xFFD9D9D9,
        0xFFD9D9D9,
        0xFFD9D9D9,
        0xFFD9D9D9,
        0xFF000000,
        0xFF333333,
        0xFFE3E3E3,
        0xFFDEDEDE,
        0xFF0066CC,
        false,
        0xFFAAAAAA,
        0.8f,
        5.0f
    );
  }

  public static Palette getBlackPreset()
  {
    return new Palette(
        "Black",
        0xFF000000,
        0xFF000000,
        0xFF000000,
        0xFF000000,
        0xFF000000,
        0xFF000000,
        0xFF000000,
        0xFFEEEEEE,
        0xFFBBBBBB,
        0xFF000000,
        0xFF181818,
        0xFF009DFF,
        false,
        0xFF2A2A2A,
        1.0f,
        1.0f
    );
  }

  public static Palette getPresetForThemeName(Context context, String themeName)
  {
    if (themeName == null || themeName.isEmpty()) themeName = "system";
    String lower = themeName.toLowerCase(java.util.Locale.ROOT).trim();
    switch (lower)
    {
      case "dark": return getDarkPreset();
      case "light": return getLightPreset();
      case "black": return getBlackPreset();
      case "altblack": return getAltBlackPreset();
      case "white": return getWhitePreset();
      case "desert": return getDesertPreset();
      case "epaper": return getEPaperPreset();
      case "epaperblack": return getEPaperBlackPreset();
      case "jungle": return getJunglePreset();
      case "monet": return getMonetSystemPreset(context);
      case "monetlight": return getMonetLightPreset();
      case "monetdark": return getMonetDarkPreset();
      case "rosepine": return getRosePinePreset();
      case "everforestlight": return getEverforestLightPreset();
      case "cobalt": return getCobaltPreset();
      case "pine": return getPinePreset();
      case "dracula": return getDraculaPreset();
      case "gradientpurplepink": return getGradientPurplePinkPreset();
      case "cyberpunk": return getCyberpunkPreset();
      case "nord": return getNordPreset();
      case "monokai": return getMonokaiPreset();
      default:
      case "system":
        return getSystemPreset(context);
    }
  }

  public static Palette getAltBlackPreset()
  {
    return new Palette(
        "Alternative Black",
        0xFF000000,
        0xFF000000,
        0xFF000000,
        0xFF000000,
        0xFF000000,
        0xFF000000,
        0xFF000000,
        0xFFEEEEEE,
        0xFFBBBBBB,
        0xFF000000,
        0xFF181818,
        0xFF009DFF,
        true,
        0xFF2A2A2A,
        1.0f,
        1.0f
    );
  }

  public static Palette getWhitePreset()
  {
    return new Palette(
        "White",
        0xFFFFFFFF,
        0xFFFFFFFF,
        0xFFFFFFFF,
        0xFFFFFFFF,
        0xFFFFFFFF,
        0xFFFFFFFF,
        0xFFFFFFFF,
        0xFF000000,
        0xFF333333,
        0xFFFFFFFF,
        0xFFF0F0F0,
        0xFF0066CC,
        true,
        0xFFEEEEEE,
        1.0f,
        5.0f
    );
  }

  public static Palette getEPaperPreset()
  {
    return new Palette(
        "ePaper",
        0xFFFFFFFF,
        0xFFFFFFFF,
        0xFFFFFFFF,
        0xFFFFFFFF,
        0xFFFFFFFF,
        0xFFFFFFFF,
        0xFFFFFFFF,
        0xFF000000,
        0xFF333333,
        0xFFFFFFFF,
        0xFFFFFFFF,
        0xFF000000,
        true,
        0xFF000000,
        2.0f,
        5.0f
    );
  }

  public static Palette getDesertPreset()
  {
    return new Palette(
        "Desert",
        0xFFFFE0B2,
        0xFFFFF3E0,
        0xFFFFEDD0,
        0xFFFFE9C6,
        0xFFFFE9C6,
        0xFFFFE9C6,
        0xFFFFE9C6,
        0xFF000000,
        0xFF333333,
        0xFFFFE0B2,
        0xFFFFEDD0,
        0xFFE65100,
        false,
        0xFFD7CCC8,
        0.0f,
        5.0f
    );
  }

  public static Palette getJunglePreset()
  {
    return new Palette(
        "Jungle",
        0xFF4DB6AC,
        0xFFE0F2F1,
        0xFFC3E7E3,
        0xFFB8E3DE,
        0xFFB8E3DE,
        0xFFB8E3DE,
        0xFFB8E3DE,
        0xFF000000,
        0xFF004D40,
        0xFF4DB6AC,
        0xFFE0F2F1,
        0xFF004D40,
        false,
        0xFF389B92,
        0.0f,
        5.0f
    );
  }

  public static Palette getMonetSystemPreset(Context context)
  {
    boolean isNight = true;
    if (context != null)
    {
      int nightFlags = context.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
      isNight = nightFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }
    return isNight ? getMonetDarkPreset("Monet (System)") : getMonetLightPreset("Monet (System)");
  }

  public static Palette getMonetLightPreset()
  {
    return getMonetLightPreset("Monet (Light)");
  }

  public static Palette getMonetLightPreset(String title)
  {
    return new Palette(
        title,
        0xFFEFE7EC,
        0xFFFBF1F6,
        0xFFE6DEE3,
        0xFFE8DEF8,
        0xFFE8DEF8,
        0xFFE8DEF8,
        0xFFE8DEF8,
        0xFF1D1B20,
        0xFF49454F,
        0xFFEFE7EC,
        0xFFFBF1F6,
        0xFF6750A4,
        false,
        0xFFCAC4D0,
        0.0f,
        5.0f
    );
  }

  public static Palette getMonetDarkPreset()
  {
    return getMonetDarkPreset("Monet (Dark)");
  }

  public static Palette getMonetDarkPreset(String title)
  {
    return new Palette(
        title,
        0xFF1D1B1E,
        0xFF2A282D,
        0xFF333036,
        0xFF4A4458,
        0xFF4A4458,
        0xFF4A4458,
        0xFF4A4458,
        0xFFE6E1E5,
        0xFFCAC4D0,
        0xFF1D1B1E,
        0xFF2A282D,
        0xFFD0BCFF,
        false,
        0xFF49454F,
        0.0f,
        5.0f
    );
  }

  public static Palette getRosePinePreset()
  {
    return new Palette(
        "Rosé Pine",
        0xFF191724,
        0xFF26233A,
        0xFF2E2B47,
        0xFF2A2740,
        0xFF2A2740,
        0xFF2A2740,
        0xFF2A2740,
        0xFFE0DEF4,
        0xFF6E6A86,
        0xFF191724,
        0xFF26233A,
        0xFFC4A7E7,
        false,
        0xFF6E6A86,
        0.0f,
        1.0f
    );
  }

  public static Palette getEverforestLightPreset()
  {
    return new Palette(
        "Everforest Light",
        0xFFF8F5E4,
        0xFFE5E2D1,
        0xFFE5E2D1,
        0xFFE5E2D1,
        0xFFE5E2D1,
        0xFFE5E2D1,
        0xFFE5E2D1,
        0xFF5C6A72,
        0xFF7B8478,
        0xFFF8F5E4,
        0xFFDBE0B6,
        0xFF828812,
        false,
        0xFFD3CEBA,
        0.0f,
        5.0f
    );
  }

  public static Palette getCobaltPreset()
  {
    return new Palette(
        "Cobalt",
        0xFF000000,
        0xFF000000,
        0xFF000000,
        0xFF000000,
        0xFF000000,
        0xFF000000,
        0xFF000000,
        0xFF95C9FF,
        0xFF66DBFF,
        0xFF000000,
        0xFF0D1B2A,
        0xFF95C9FF,
        false,
        0xFF333349,
        0.0f,
        5.0f
    );
  }

  public static Palette getPinePreset()
  {
    return new Palette(
        "Pine",
        0xFF000000,
        0xFF000000,
        0xFF000000,
        0xFF000000,
        0xFF000000,
        0xFF000000,
        0xFF000000,
        0xFF91D7A6,
        0xFF63EA8B,
        0xFF000000,
        0xFF0A1F0D,
        0xFF91D7A6,
        false,
        0xFF2F301E,
        0.0f,
        5.0f
    );
  }

  public static Palette getEPaperBlackPreset()
  {
    return new Palette(
        "ePaper Black",
        0xFF000000,
        0xFF000000,
        0xFF000000,
        0xFF000000,
        0xFF000000,
        0xFF000000,
        0xFF000000,
        0xFFFFFFFF,
        0xFFFFFFFF,
        0xFF000000,
        0xFF000000,
        0xFFFFFFFF,
        true,
        0xFFFFFFFF,
        1.0f,
        5.0f
    );
  }

  public static Palette getDraculaPreset()
  {
    return new Palette(
        "Dracula",
        DRACULA_KEYBOARD_BG,
        DRACULA_KEY_NORMAL,
        DRACULA_KEY_NORMAL, // Match Space in Dracula theme
        0xFF7446A8, // Shift (Dracula action key in themes.xml)
        0xFF7446A8, // Ctrl
        0xFF7446A8, // Enter
        0xFF7446A8, // Backspace
        DRACULA_LABEL_COLOR,
        DRACULA_SUBLABEL_COLOR,
        0xFF21222C,
        0xFF343746,
        0xFF50FA7B,
        false,
        0xFF44475A,
        0.0f,
        8.0f
    );
  }

  public static Palette getGradientPurplePinkPreset()
  {
    return new Palette(
        "Gradient Purple Pink",
        0xFF251A3A,
        0xB3262033, // Match themes.xml colorKey (translucent so gradient shines through)
        0xC72D253D, // Match themes.xml colorKeySpaceBar
        0xD13B2D50, // Match themes.xml colorKeyAction
        0xD13B2D50,
        0xD13B2D50,
        0xD13B2D50,
        0xFFFFFFFF,
        0xFFB088D3,
        0xFF251A3A,
        0x803B2D50,
        0xFFFFFFFF,
        false,
        0xFF5E3A7D,
        0.0f,
        8.0f,
        true,
        0xFF8A2BE2, // keyboardGradientStart: #8A2BE2
        0xFFAF6679  // keyboardGradientEnd: #AF6679
    );
  }

  public static Palette getGradientSunsetPreset()
  {
    return new Palette(
        "Sunset Glow",
        0xFF1A0B2E,
        0xA02D154B,
        0xB0361A5C,
        0xB04A154B,
        0xB04A154B,
        0xB04A154B,
        0xB04A154B,
        0xFFFFFFFF,
        0xFFFFB088,
        0xFF1A0B2E,
        0x804A154B,
        0xFFFF884D,
        false,
        0xFFFF512F,
        0.0f,
        8.0f,
        true,
        0xFFFF512F,
        0xFFDD2476
    );
  }

  public static Palette getGradientOceanPreset()
  {
    return new Palette(
        "Ocean Breeze",
        0xFF0A192F,
        0xA0112240,
        0xB01A365D,
        0xB00D3B66,
        0xB00D3B66,
        0xB00D3B66,
        0xB00D3B66,
        0xFFFFFFFF,
        0xFF64FFDA,
        0xFF0A192F,
        0x800D3B66,
        0xFF00F0FF,
        false,
        0xFF00C6FF,
        0.0f,
        8.0f,
        true,
        0xFF0072FF,
        0xFF00C6FF
    );
  }

  public static Palette getCyberpunkPreset()
  {
    return new Palette(
        "Cyberpunk",
        0xFF120E24,
        0xFF1E163B,
        0xFF2A1B4E,
        0xFF00F0FF,
        0xFF7B2CBF,
        0xFFFF007F,
        0xFFFF3864,
        0xFFF0F8FF,
        0xFF9D8DF1,
        0xFF0B0819,
        0xFF241846,
        0xFF00F0FF,
        true,
        0xFF00F0FF,
        1.0f,
        6.0f
    );
  }

  public static Palette getNordPreset()
  {
    return new Palette(
        "Nord",
        0xFF2E3440,
        0xFF3B4252,
        0xFF434C5E,
        0xFF4C566A,
        0xFF4C566A,
        0xFFA3BE8C,
        0xFFBF616A,
        0xFFECEFF4,
        0xFFD8DEE9,
        0xFF242933,
        0xFF3B4252,
        0xFF88C0D0,
        false,
        0xFF4C566A,
        0.0f,
        6.0f
    );
  }

  public static Palette getMonokaiPreset()
  {
    return new Palette(
        "Monokai Pro",
        0xFF2D2A2E,
        0xFF403E41,
        0xFF49464A,
        0xFF5C585C,
        0xFF5C585C,
        0xFFA9DC76,
        0xFFFF6188,
        0xFFFCFCFA,
        0xFF939293,
        0xFF221F22,
        0xFF403E41,
        0xFFFFD866,
        false,
        0xFF5C585C,
        0.0f,
        6.0f
    );
  }

  public static List<Palette> getAllBuiltinPresets(Context context)
  {
    List<Palette> list = new ArrayList<Palette>();
    list.add(getSystemPreset(context));
    list.add(getDarkPreset());
    list.add(getLightPreset());
    list.add(getBlackPreset());
    list.add(getAltBlackPreset());
    list.add(getWhitePreset());
    list.add(getEPaperPreset());
    list.add(getDesertPreset());
    list.add(getJunglePreset());
    list.add(getMonetSystemPreset(context));
    list.add(getMonetLightPreset());
    list.add(getMonetDarkPreset());
    list.add(getRosePinePreset());
    list.add(getEverforestLightPreset());
    list.add(getCobaltPreset());
    list.add(getPinePreset());
    list.add(getEPaperBlackPreset());
    list.add(getDraculaPreset());
    list.add(getGradientPurplePinkPreset());
    list.add(getGradientSunsetPreset());
    list.add(getGradientOceanPreset());
    list.add(getCyberpunkPreset());
    list.add(getNordPreset());
    list.add(getMonokaiPreset());
    return list;
  }
}
