package typodev.keyboard;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Canvas;
import android.graphics.Insets;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.inputmethodservice.InputMethodService;
import android.os.Build.VERSION;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;
import java.util.Arrays;
import java.util.List;

public class Keyboard2View extends View
  implements View.OnTouchListener, Pointers.IPointerEventHandler
{
  private KeyboardData _keyboard;

  /** The key holding the shift key is used to set shift state from
      autocapitalisation. */
  private KeyboardData.Key _shift_key;

  /** Used to add fake pointers. */
  private KeyboardData.Key _compose_key;

  private Pointers _pointers;

  private Pointers.Modifiers _mods;

  private static int _currentWhat = 0;

  private Config _config;

  private float _keyWidth;
  private float _mainLabelSize;
  private float _subLabelSize;
  private float _marginRight;
  private float _marginLeft;
  private float _marginBottom;
  private int _insets_left = 0;
  private int _insets_right = 0;
  private int _insets_bottom = 0;

  private Theme _theme;
  private Theme.Computed _tc;

  public static final class PreviewThemeOverride
  {
    public int keyboardBg;
    public int keyNormal;
    public int keySpace;
    public int keyShift;
    public int keyCtrl;
    public int keyEnter;
    public int keyBackspace;
    public int labelColor;
    public int subLabelColor;
    public boolean borderEnabled;
    public int borderColor;
    public float borderWidthDp;
    public float borderRadiusDp;
    public boolean bottomBorderEnabled;
    public int bottomBorderColor;
    public float bottomBorderHeightDp;
    public float outerCornerRadiusDp;
    public boolean hasGradient;
    public int gradientStart;
    public int gradientEnd;
    public int actionLabelColor = CustomThemeStore.DEFAULT_ACTION_LABEL_COLOR;
    public int shiftTextColor = CustomThemeStore.DEFAULT_ACTION_LABEL_COLOR;
    public int ctrlTextColor = CustomThemeStore.DEFAULT_ACTION_LABEL_COLOR;
    public int enterTextColor = CustomThemeStore.DEFAULT_ACTION_LABEL_COLOR;
    public int backspaceTextColor = CustomThemeStore.DEFAULT_ACTION_LABEL_COLOR;
    public int lockedTextColor = CustomThemeStore.DEFAULT_LOCKED_TEXT_COLOR;
    public int activatedTextColor = CustomThemeStore.DEFAULT_ACTIVATED_TEXT_COLOR;
    public int keyPressedBgColor = CustomThemeStore.DEFAULT_KEY_PRESSED_BG_COLOR;
    public int keyPressedTextColor = CustomThemeStore.DEFAULT_KEY_PRESSED_TEXT_COLOR;
    public int popupBgColor = CustomThemeStore.DEFAULT_POPUP_BG_COLOR;
    public int popupTextColor = CustomThemeStore.DEFAULT_POPUP_TEXT_COLOR;
    public boolean bottomBorderShift = true;
    public boolean bottomBorderCtrl = true;
    public boolean bottomBorderBackspace = true;
    public boolean bottomBorderSpace = true;
    public boolean bottomBorderEnter = true;
  }

  private final android.graphics.Path _tmpCornerPath = new android.graphics.Path();
  private final float[] _tmpRadii = new float[8];

  public float getOuterCornerRadius()
  {
    if (_previewTheme != null)
    {
      return _previewTheme.outerCornerRadiusDp * getResources().getDisplayMetrics().density;
    }
    CustomThemeStore store = CustomThemeStore.instance(getContext());
    if (store != null)
    {
      return store.getOuterCornerRadiusDp() * getResources().getDisplayMetrics().density;
    }
    return 0f;
  }

  private PreviewThemeOverride _previewTheme = null;
  private boolean _isPreviewMode = false;

  public void setPreviewMode(boolean isPreview)
  {
    _isPreviewMode = isPreview;
    if (isPreview)
    {
      _insets_left = 0;
      _insets_right = 0;
      _insets_bottom = 0;
      _marginBottom = 0;
      requestLayout();
    }
  }

  public boolean isPreviewMode()
  {
    return _isPreviewMode || (_previewTheme != null);
  }

  public void setPreviewTheme(PreviewThemeOverride pt)
  {
    _previewTheme = pt;
    if (pt != null)
    {
      _isPreviewMode = true;
      _insets_left = 0;
      _insets_right = 0;
      _insets_bottom = 0;
      _marginBottom = 0;
    }
    if (_keyboard != null && _theme != null && _config != null && _keyWidth > 0)
    {
      _tc = new Theme.Computed(_theme, _config, _keyWidth, _keyboard, _previewTheme);
    }
    invalidate();
  }

  public Theme getTheme()
  {
    return _theme;
  }

  public Config getConfig()
  {
    return _config;
  }

  public float getKeyWidth()
  {
    return _keyWidth;
  }

  private KeyPreviewPopup _keyPreviewPopup;
  private KeyboardData.Key _lastPreviewKey = null;

  private static RectF _tmpRect = new RectF();

  enum Vertical
  {
    TOP,
    CENTER,
    BOTTOM
  }

  public Keyboard2View(Context context, AttributeSet attrs)
  {
    super(context, attrs);
    _config = Config.globalConfig();
    Context themeContext = context;
    android.content.res.TypedArray check = context.getTheme().obtainStyledAttributes(attrs, R.styleable.keyboard, 0, 0);
    boolean hasKeyboardAttrs = check.hasValue(R.styleable.keyboard_colorKeyboard) || check.hasValue(R.styleable.keyboard_colorKey);
    check.recycle();
    if (!hasKeyboardAttrs)
    {
      int themeResId = (_config != null && _config.theme != 0) ? _config.theme : R.style.Dark;
      themeContext = new android.view.ContextThemeWrapper(context, themeResId);
    }
    _theme = new Theme(themeContext, attrs);
    _pointers = new Pointers(this, _config);
    _keyPreviewPopup = new KeyPreviewPopup(getContext());
    refresh_navigation_bar(context);
    setOnTouchListener(this);
    setSoundEffectsEnabled(false);
    int layout_id = (attrs == null) ? 0 :
      attrs.getAttributeResourceValue(null, "layout", 0);
    if (layout_id == 0)
      reset();
    else
      setKeyboard(KeyboardData.load(getResources(), layout_id));
  }

  private Window getParentWindow(Context context)
  {
    if (context instanceof InputMethodService)
      return ((InputMethodService)context).getWindow().getWindow();
    if (context instanceof ContextWrapper)
      return getParentWindow(((ContextWrapper)context).getBaseContext());
    return null;
  }

  public void refresh_navigation_bar(Context context)
  {
    if (VERSION.SDK_INT < 21)
      return;
    // The intermediate Window is a [Dialog].
    Window w = getParentWindow(context);
    if (w == null)
      return;
    w.setNavigationBarColor(_theme.colorNavBar);
    if (VERSION.SDK_INT < 26)
      return;
    int uiFlags = getSystemUiVisibility();
    if (_theme.isLightNavBar)
      uiFlags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
    else
      uiFlags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
    setSystemUiVisibility(uiFlags);
  }

  public void setKeyboard(KeyboardData kw)
  {
    _keyboard = kw;
    _shift_key = _keyboard.findKeyWithValue(KeyValue.SHIFT);
    _compose_key = _keyboard.findKeyWithValue(KeyValue.COMPOSE);
    if (!isPreviewMode()) KeyModifier.set_modmap(_keyboard.modmap);
    reset();
  }

  public void setConfig(Config config)
  {
    if (_pointers != null) _pointers.clear();
    _config = config;
    _pointers = (config == null) ? null : new Pointers(this, config);
    reset();
  }

  private Config.IKeyEventHandler getKeyEventHandler()
  {
    // A preview must never dispatch taps to the active editor or IME service.
    if (isPreviewMode()) return null;
    if (_config != null && _config.handler != null)
      return _config.handler;
    return Config.getGlobalHandler();
  }

  public void reset()
  {
    dismissKeyPreview();
    _mods = Pointers.Modifiers.EMPTY;
    if (_pointers != null) _pointers.clear();
    Config global = Config.globalConfig();
    if (!isPreviewMode() && global != null && _config != global)
    {
      _config = global;
      _pointers = new Pointers(this, _config);
    }
    if (_pointers == null && _config != null) _pointers = new Pointers(this, _config);
    if (_keyboard != null && _theme != null && _config != null && _keyWidth > 0)
    {
      if (_previewTheme != null)
        _tc = new Theme.Computed(_theme, _config, _keyWidth, _keyboard, _previewTheme);
      else
        _tc = new Theme.Computed(_theme, _config, _keyWidth, _keyboard);
    }
    requestLayout();
    invalidate();
  }

  void set_fake_ptr_latched(KeyboardData.Key key, KeyValue kv, boolean latched,
      boolean lock)
  {
    if (_keyboard == null || key == null)
      return;
    _pointers.set_fake_pointer_state(key, kv, latched, lock);
  }

  /** Called by auto-capitalisation. */
  public void set_shift_state(boolean latched, boolean lock)
  {
    set_fake_ptr_latched(_shift_key, KeyValue.SHIFT, latched, lock);
  }

  /** Called from [KeyEventHandler]. */
  public void set_compose_pending(boolean pending)
  {
    set_fake_ptr_latched(_compose_key, KeyValue.COMPOSE, pending, false);
  }

  /** Called from [Keybard2.onUpdateSelection].  */
  public void set_selection_state(boolean selection_state)
  {
    if (_config.editor_config.selection_mode_enabled)
      set_fake_ptr_latched(KeyboardData.Key.EMPTY,
          KeyValue.SELECTION_MODE, selection_state, true);
  }

  public KeyValue modifyKey(KeyValue k, Pointers.Modifiers mods)
  {
    return KeyModifier.modify(k, mods);
  }

  @Override
  public void onPointerDown(KeyValue k, boolean isSwipe)
  {
    onPointerDown(_lastPreviewKey, k, isSwipe);
  }

  @Override
  public void onPointerDown(KeyboardData.Key key, KeyValue k, boolean isSwipe)
  {
    if (key != null)
      _lastPreviewKey = key;
    updateFlags();
    Config.IKeyEventHandler handler = getKeyEventHandler();
    if (handler != null)
      handler.key_down(k, isSwipe);
    showKeyPreview(key != null ? key : _lastPreviewKey, k);
    invalidate();
    vibrate();
    playSound(k);
  }

  public void onPointerUp(KeyValue k, Pointers.Modifiers mods)
  {
    // [key_up] must be called before [updateFlags]. The latter might disable
    // flags.
    Config.IKeyEventHandler handler = getKeyEventHandler();
    if (handler != null)
      handler.key_up(k, mods);
    updateFlags();
    invalidate();
  }

  public void onPointerHold(KeyValue k, Pointers.Modifiers mods)
  {
    Config.IKeyEventHandler handler = getKeyEventHandler();
    if (handler != null)
      handler.key_up(k, mods);
    updateFlags();
  }

  public void onPointerFlagsChanged(boolean shouldVibrate)
  {
    updateFlags();
    invalidate();
    if (shouldVibrate)
      vibrate();
  }

  private void updateFlags()
  {
    _mods = _pointers.getModifiers();
    Config.IKeyEventHandler handler = getKeyEventHandler();
    if (handler != null)
      handler.mods_changed(_mods);
  }

  @Override
  public boolean onTouch(View v, MotionEvent event)
  {
    int p;
    switch (event.getActionMasked())
    {
      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_POINTER_UP:
        dismissKeyPreview();
        _pointers.onTouchUp(event.getPointerId(event.getActionIndex()));
        break;
      case MotionEvent.ACTION_DOWN:
      case MotionEvent.ACTION_POINTER_DOWN:
        p = event.getActionIndex();
        float tx = event.getX(p);
        float ty = event.getY(p);
        KeyboardData.Key key = getKeyAtPosition(tx, ty);
        if (key != null)
        {
          _lastPreviewKey = key;
          _pointers.onTouchDown(tx, ty, event.getPointerId(p), key);
        }
        break;
      case MotionEvent.ACTION_MOVE:
        for (p = 0; p < event.getPointerCount(); p++)
        {
          float mx = event.getX(p);
          float my = event.getY(p);
          _pointers.onTouchMove(mx, my, event.getPointerId(p));
        }
        break;
      case MotionEvent.ACTION_CANCEL:
        dismissKeyPreview();
        _pointers.onTouchCancel();
        break;
      default:
        return (false);
    }
    return (true);
  }

  public RectF getKeyBounds(KeyboardData.Key targetKey)
  {
    if (_keyboard == null || _tc == null) return null;
    float y = _tc.margin_top;
    for (KeyboardData.Row row : _keyboard.rows)
    {
      y += row.shift * _tc.row_height;
      float x = _marginLeft + _tc.margin_left;
      float keyH = row.height * _tc.row_height - _tc.vertical_margin;
      for (KeyboardData.Key k : row.keys)
      {
        x += k.shift * _keyWidth;
        float keyW = _keyWidth * k.width - _tc.horizontal_margin;
        if (k == targetKey)
        {
          return new RectF(x, y, x + keyW, y + keyH);
        }
        x += _keyWidth * k.width;
      }
      y += row.height * _tc.row_height;
    }
    return null;
  }

  private void showKeyPreview(KeyboardData.Key key)
  {
    if (key == null || key.keys == null || key.keys.length == 0 || key.keys[0] == null)
      return;
    KeyValue kv = modifyKey(key.keys[0], _mods);
    showKeyPreview(key, kv);
  }

  private void showKeyPreview(KeyboardData.Key key, KeyValue kv)
  {
    try
    {
      Config cfg = isPreviewMode() ? _config : Config.globalConfig();
      if (cfg == null) cfg = _config;
      if (cfg == null || !cfg.popup_on_keypress)
        return;
      if (key == null || kv == null)
      {
        dismissKeyPreview();
        return;
      }
      if (isShiftKey(key) || isCtrlKey(key) || isEnterKey(key) || isBackspaceKey(key) || key.role == KeyboardData.Key.Role.Space_bar)
      {
        dismissKeyPreview();
        return;
      }

      KeyValue.Kind kind = kv.getKind();
      if (kind != KeyValue.Kind.Char &&
          kind != KeyValue.Kind.String &&
          kind != KeyValue.Kind.Hangul_initial &&
          kind != KeyValue.Kind.Hangul_medial)
      {
        dismissKeyPreview();
        return;
      }

      if (kv.hasFlagsAny(KeyValue.FLAG_KEY_FONT))
      {
        dismissKeyPreview();
        return;
      }

      String text = kv.getString();
      if (text == null || text.trim().isEmpty())
      {
        dismissKeyPreview();
        return;
      }

      _lastPreviewKey = key;
      RectF bounds = getKeyBounds(key);
      if (bounds == null)
        return;

      int bgColor;
      int textColor;
      if (_previewTheme != null)
      {
        bgColor = _previewTheme.popupBgColor;
        textColor = _previewTheme.popupTextColor;
      }
      else if (CustomThemeStore.instance(getContext()).isCustomThemeActive(_config))
      {
        CustomThemeStore cs = CustomThemeStore.instance(getContext());
        bgColor = cs.getPopupBgColor();
        textColor = cs.getPopupTextColor();
      }
      else
      {
        bgColor = (_theme.colorKeyActivated != 0) ? _theme.colorKeyActivated : _theme.colorKey;
        textColor = _theme.labelColor;
      }

      if (_keyPreviewPopup != null)
      {
        _keyPreviewPopup.show(this, text, bounds.left, bounds.top, bounds.width(), bounds.height(), bgColor, textColor);
      }
    }
    catch (Throwable ignored) {}
  }

  private void dismissKeyPreview()
  {
    _lastPreviewKey = null;
    if (_keyPreviewPopup != null)
    {
      _keyPreviewPopup.dismiss();
    }
  }

  public KeyboardData.Key getKeyAtPosition(float tx, float ty)
  {
    if (_keyboard == null || _tc == null) return null;

    float y = _tc.margin_top;
    KeyboardData.Key bestKey = null;
    float minDistance = Float.MAX_VALUE;

    int rowCount = _keyboard.rows.size();
    for (int rIdx = 0; rIdx < rowCount; rIdx++)
    {
      KeyboardData.Row row = _keyboard.rows.get(rIdx);
      float rowTop = y + row.shift * _tc.row_height;
      float rowHeight = row.height * _tc.row_height;
      float rowBottom = rowTop + rowHeight;
      float x = _marginLeft + _tc.margin_left;

      int keyCount = row.keys.size();
      for (int kIdx = 0; kIdx < keyCount; kIdx++)
      {
        KeyboardData.Key k = row.keys.get(kIdx);
        float keyLeft = x + k.shift * _keyWidth;
        float keyRight = keyLeft + k.width * _keyWidth;

        // Exact hit inside key bounding box
        if (tx >= keyLeft && tx < keyRight && ty >= rowTop && ty < rowBottom)
        {
          return k;
        }

        // Distance to key center for fallback (edges/margins)
        float centerX = (keyLeft + keyRight) / 2f;
        float centerY = (rowTop + rowBottom) / 2f;
        float dist = (tx - centerX) * (tx - centerX) + (ty - centerY) * (ty - centerY);
        if (dist < minDistance)
        {
          minDistance = dist;
          bestKey = k;
        }

        x += (k.shift + k.width) * _keyWidth;
      }
      y += (row.shift + row.height) * _tc.row_height;
    }

    // Fallback: if tap was within reasonable range (e.g. edge or margin), return closest key
    if (bestKey != null && minDistance < (_keyWidth * _keyWidth * 9.0f))
    {
      return bestKey;
    }
    return null;
  }

  private void vibrate()
  {
    Config cfg = isPreviewMode() ? _config : Config.globalConfig();
    if (cfg == null) cfg = _config;
    VibratorCompat.vibrate(this, cfg);
  }

  private void playSound(KeyValue k)
  {
    Config cfg = isPreviewMode() ? _config : Config.globalConfig();
    if (cfg == null) cfg = _config;
    SoundFeedbackManager.getInstance(getContext()).play(k, cfg);
  }

  @Override
  public void onMeasure(int wSpec, int hSpec)
  {
    DisplayMetrics dm = getContext().getResources().getDisplayMetrics();
    int width = MeasureSpec.getSize(wSpec);
    if (width <= 0)
      width = dm.widthPixels;
    if (_config == null) _config = Config.globalConfig();
    if (_config == null)
    {
      setMeasuredDimension(width, 0);
      return;
    }
    if (isPreviewMode())
    {
      _insets_left = 0;
      _insets_right = 0;
      _insets_bottom = 0;
      _marginLeft = _config.horizontal_margin;
      _marginRight = _config.horizontal_margin;
      _marginBottom = _config.margin_bottom;
    }
    else
    {
      _marginLeft = Math.max(_config.horizontal_margin, _insets_left);
      _marginRight = Math.max(_config.horizontal_margin, _insets_right);
      _marginBottom = _config.margin_bottom + _insets_bottom;
    }
    if (_keyboard == null)
    {
      setMeasuredDimension(width, 0);
      return;
    }
    _keyWidth = (width - _marginLeft - _marginRight) / _keyboard.keysWidth;
    if (_previewTheme != null)
    {
      _tc = new Theme.Computed(_theme, _config, _keyWidth, _keyboard, _previewTheme);
    }
    else
    {
      _tc = new Theme.Computed(_theme, _config, _keyWidth, _keyboard);
    }
    // Compute the size of labels based on the width or the height of keys. The
    // margin around keys is taken into account. Keys normal aspect ratio is
    // assumed to be 3/2 for a 10 columns layout. It's generally more, the
    // width computation is useful when the keyboard is unusually high.
    float labelBaseSize = Math.min(
        _tc.row_height - _tc.vertical_margin,
        (width / 10 - _tc.horizontal_margin) * 3/2
        ) * _config.characterSize;
    _mainLabelSize = labelBaseSize * _config.labelTextSize;
    _subLabelSize = labelBaseSize * _config.sublabelTextSize;

    if (isPreviewMode())
    {
      float totalRowsHeight = 0f;
      for (KeyboardData.Row r : _keyboard.rows)
      {
        totalRowsHeight += r.height + r.shift;
      }
      int height = (int)(_tc.row_height * totalRowsHeight
          + _config.marginTop + _config.margin_bottom);
      setMeasuredDimension(width, height);
      return;
    }

    int height =
      (int)(_tc.row_height * _keyboard.keysHeight
          + _config.marginTop + _marginBottom);
    setMeasuredDimension(width, height);
  }

  public int getBottomMargin()
  {
    return (int)_marginBottom;
  }

  Rect _cached_exclusion_rect = new Rect();
  List<Rect> _cached_exclusion_rects = Arrays.asList(_cached_exclusion_rect);
  @Override
  public void onLayout(boolean changed, int left, int top, int right, int bottom)
  {
    if (!changed)
      return;
    // Since SDK 30, this is done automatically:
    // https://android.googlesource.com/platform/frameworks/base/+/android11-release/core/java/android/inputmethodservice/InputMethodService.java#852
    if (VERSION.SDK_INT == 29)
    {
      // Disable the back-gesture on the keyboard area
      _cached_exclusion_rect.set(
          left + (int)_marginLeft,
          top + (int)_config.marginTop,
          right - (int)_marginRight,
          bottom - (int)_marginBottom);
      setSystemGestureExclusionRects(_cached_exclusion_rects);
    }
  }

  @Override
  public WindowInsets onApplyWindowInsets(WindowInsets wi)
  {
    if (isPreviewMode())
    {
      _insets_left = 0;
      _insets_right = 0;
      _insets_bottom = 0;
      return wi;
    }
    // LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS is set in [Keyboard2#updateSoftInputWindowLayoutParams] for SDK_INT >= 35.
    if (VERSION.SDK_INT < 35)
      return wi;
    int insets_types =
      WindowInsets.Type.systemBars()
      | WindowInsets.Type.displayCutout();
    Insets insets = wi.getInsets(insets_types);
    _insets_left = insets.left;
    _insets_right = insets.right;
    _insets_bottom = insets.bottom;
    return WindowInsets.CONSUMED;
  }

  /** Horizontal and vertical position of the 9 indexes. */
  static final Paint.Align[] LABEL_POSITION_H = new Paint.Align[]{
    Paint.Align.CENTER, Paint.Align.LEFT, Paint.Align.RIGHT, Paint.Align.LEFT,
    Paint.Align.RIGHT, Paint.Align.LEFT, Paint.Align.RIGHT,
    Paint.Align.CENTER, Paint.Align.CENTER
  };

  static final Vertical[] LABEL_POSITION_V = new Vertical[]{
    Vertical.CENTER, Vertical.TOP, Vertical.TOP, Vertical.BOTTOM,
    Vertical.BOTTOM, Vertical.CENTER, Vertical.CENTER, Vertical.TOP,
    Vertical.BOTTOM
  };

  public static boolean isShiftKey(KeyboardData.Key k)
  {
    if (k == null) return false;
    if (k.keys[0] != null && k.keys[0].getKind() == KeyValue.Kind.Modifier && k.keys[0].getModifier() == KeyValue.Modifier.SHIFT)
      return true;
    return k.hasValue(KeyValue.SHIFT);
  }

  public static boolean isCtrlKey(KeyboardData.Key k)
  {
    if (k == null) return false;
    if (k.keys[0] != null && k.keys[0].getKind() == KeyValue.Kind.Modifier && k.keys[0].getModifier() == KeyValue.Modifier.CTRL)
      return true;
    return false;
  }

  public static boolean isEnterKey(KeyboardData.Key k)
  {
    if (k == null) return false;
    if (k.keys[0] != null)
    {
      if (k.keys[0].getKind() == KeyValue.Kind.Event && k.keys[0].getEvent() == KeyValue.Event.ACTION)
        return true;
      if (k.keys[0].getKind() == KeyValue.Kind.Keyevent && k.keys[0].getKeyevent() == KeyEvent.KEYCODE_ENTER)
        return true;
    }
    return false;
  }

  public static boolean isBackspaceKey(KeyboardData.Key k)
  {
    if (k == null) return false;
    if (k.keys[0] != null)
    {
      if (k.keys[0].getKind() == KeyValue.Kind.Editing && k.keys[0].getEditing() == KeyValue.Editing.BACKSPACE)
        return true;
      if (k.keys[0].getKind() == KeyValue.Kind.Keyevent && k.keys[0].getKeyevent() == KeyEvent.KEYCODE_DEL)
        return true;
    }
    return false;
  }

  public static boolean isFnKey(KeyboardData.Key k)
  {
    if (k == null) return false;
    if (k.keys[0] != null && k.keys[0].getKind() == KeyValue.Kind.Modifier && k.keys[0].getModifier() == KeyValue.Modifier.FN)
      return true;
    return false;
  }

  public static boolean isActionKey(KeyboardData.Key k)
  {
    if (k == null) return false;
    return isShiftKey(k) || isCtrlKey(k) || isFnKey(k) || isEnterKey(k) || isBackspaceKey(k) || k.role == KeyboardData.Key.Role.Action;
  }

  @Override
  protected void onDraw(Canvas canvas)
  {
    if (_keyboard == null || _tc == null)
      return;
    boolean isCustom = (_previewTheme != null) || CustomThemeStore.instance(getContext()).isCustomThemeActive(_config);
    if (_previewTheme != null)
    {
      if (_previewTheme.hasGradient)
      {
        float h = getHeight();
        if (h <= 0 && _tc != null && _keyboard != null)
          h = _tc.margin_top + _tc.row_height * _keyboard.keysHeight + _config.margin_bottom;
        Paint gp = new Paint();
        gp.setShader(new LinearGradient(0, 0, 0, h > 0 ? h : 400, _previewTheme.gradientStart, _previewTheme.gradientEnd, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, getWidth(), getHeight(), gp);
      }
      else
      {
        canvas.drawColor(_previewTheme.keyboardBg);
      }
    }
    else if (isCustom)
    {
      CustomThemeStore store = CustomThemeStore.instance(getContext());
      if (store.hasGradient())
      {
        float h = getHeight();
        if (h <= 0 && _tc != null && _keyboard != null)
          h = _tc.margin_top + _tc.row_height * _keyboard.keysHeight + _config.margin_bottom;
        Paint gp = new Paint();
        gp.setShader(new LinearGradient(0, 0, 0, h > 0 ? h : 400, store.getGradientStart(), store.getGradientEnd(), Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, getWidth(), getHeight(), gp);
      }
      else
      {
        canvas.drawColor(store.getKeyboardBg());
      }
    }
    else if (_tc.keyboard_background_paint != null)
    {
      canvas.drawRect(
              0,
              0,
              getWidth(),
              getHeight(),
              _tc.keyboard_background_paint);
    }

    float y = _tc.margin_top;
    int rowCount = _keyboard.rows.size();
    for (int rIdx = 0; rIdx < rowCount; rIdx++)
    {
      KeyboardData.Row row = _keyboard.rows.get(rIdx);
      y += row.shift * _tc.row_height;
      float x = _marginLeft + _tc.margin_left;
      float keyH = row.height * _tc.row_height - _tc.vertical_margin;
      int keyCount = row.keys.size();
      for (int kIdx = 0; kIdx < keyCount; kIdx++)
      {
        KeyboardData.Key k = row.keys.get(kIdx);
        x += k.shift * _keyWidth;
        float keyW = _keyWidth * k.width - _tc.horizontal_margin;
        boolean isKeyDown = _pointers.isKeyDown(k);
        Theme.Computed.Key tc_key;
        if (isKeyDown)
          tc_key = _tc.key_activated;
        else if (isCustom)
        {
          if (isShiftKey(k)) tc_key = _tc.key_shift;
          else if (isCtrlKey(k)) tc_key = _tc.key_ctrl;
          else if (isEnterKey(k)) tc_key = _tc.key_enter;
          else if (isBackspaceKey(k)) tc_key = _tc.key_backspace;
          else switch (k.role)
          {
            case Action: tc_key = _tc.key_action; break;
            case Space_bar: tc_key = _tc.key_space_bar; break;
            case Suggestion: tc_key = _tc.key_suggestion; break;
            default:
            case Normal: tc_key = _tc.key; break;
          }
        }
        else
          switch (k.role)
          {
            case Action: tc_key = _tc.key_action; break;
            case Space_bar: tc_key = _tc.key_space_bar; break;
            case Suggestion: tc_key = _tc.key_suggestion; break;
            default:
            case Normal: tc_key = _tc.key; break;
          }

        boolean isBottomLeft = isCtrlKey(k) || (rIdx == rowCount - 1 && kIdx == 0);
        boolean isBottomRight = isEnterKey(k) || (rIdx == rowCount - 1 && kIdx == keyCount - 1);
        drawKeyFrame(canvas, x, y, keyW, keyH, tc_key, isBottomLeft, isBottomRight);
        if (k.role == KeyboardData.Key.Role.Space_bar)
        {
          drawSpaceBarLabel(canvas, k, x, y, keyW, keyH, isKeyDown, tc_key);
        }
        else if (k.keys[0] != null)
          drawLabel(canvas, k.keys[0], keyW / 2f + x, y, keyW, keyH, isKeyDown, tc_key, k);

        for (int i = 1; i < 9; i++)
        {
          if (k.keys[i] != null)
            drawSubLabel(canvas, k.keys[i], x, y, keyW, keyH, i, isKeyDown, tc_key, isBottomLeft, isBottomRight, k);
        }
        drawIndication(canvas, k, x, y, keyW, keyH, _tc);
        x += _keyWidth * k.width;
      }
      y += row.height * _tc.row_height;
    }
  }

  @Override
  public void onDetachedFromWindow()
  {
    super.onDetachedFromWindow();
    _pointers.clear();
    dismissKeyPreview();
  }

  /** Draw borders and background of the key. */
  void drawKeyFrame(Canvas canvas, float x, float y, float keyW, float keyH,
      Theme.Computed.Key tc)
  {
    drawKeyFrame(canvas, x, y, keyW, keyH, tc, false, false);
  }

  void drawKeyFrame(Canvas canvas, float x, float y, float keyW, float keyH,
      Theme.Computed.Key tc, boolean isBottomLeftCorner, boolean isBottomRightCorner)
  {
    float r = tc.border_radius;
    float w = tc.border_width;
    float padding = w / 2.f;
    _tmpRect.set(x + padding, y + padding, x + keyW - padding, y + keyH - padding);

    float outerRadius = getOuterCornerRadius();
    boolean hasCustomCorner = (outerRadius > 0) && (isBottomLeftCorner || isBottomRightCorner);

    if (hasCustomCorner)
    {
      _tmpCornerPath.reset();
      float tl = r;
      float tr = r;
      float br = isBottomRightCorner ? Math.max(r, outerRadius) : r;
      float bl = isBottomLeftCorner ? Math.max(r, outerRadius) : r;
      _tmpRadii[0] = tl; _tmpRadii[1] = tl;
      _tmpRadii[2] = tr; _tmpRadii[3] = tr;
      _tmpRadii[4] = br; _tmpRadii[5] = br;
      _tmpRadii[6] = bl; _tmpRadii[7] = bl;
      _tmpCornerPath.addRoundRect(_tmpRect, _tmpRadii, android.graphics.Path.Direction.CW);
      canvas.drawPath(_tmpCornerPath, tc.bg_paint);

      if (w > 0.f)
      {
        if (tc.border_left_paint != null && tc.border_left_paint.getStrokeWidth() == 0
            && tc.border_bottom_paint != null && tc.border_bottom_paint.getStrokeWidth() > 0)
        {
          canvas.save();
          float clipTop = y + keyH - tc.border_bottom_paint.getStrokeWidth() - padding;
          canvas.clipRect(x, clipTop, x + keyW, y + keyH);
          canvas.drawPath(_tmpCornerPath, tc.border_bottom_paint);
          canvas.restore();
        }
        else
        {
          Paint sp = (tc.border_left_paint != null && tc.border_left_paint.getStrokeWidth() > 0)
              ? tc.border_left_paint : tc.border_bottom_paint;
          if (sp != null && sp.getStrokeWidth() > 0)
          {
            canvas.drawPath(_tmpCornerPath, sp);
          }
        }
      }
    }
    else
    {
      canvas.drawRoundRect(_tmpRect, r, r, tc.bg_paint);
      if (w > 0.f)
      {
        boolean isUniformOutline = (tc.border_left_paint != null && tc.border_top_paint != null
            && tc.border_right_paint != null && tc.border_bottom_paint != null
            && tc.border_left_paint.getColor() == tc.border_top_paint.getColor()
            && tc.border_left_paint.getColor() == tc.border_right_paint.getColor()
            && tc.border_left_paint.getColor() == tc.border_bottom_paint.getColor()
            && tc.border_left_paint.getStrokeWidth() == tc.border_top_paint.getStrokeWidth()
            && tc.border_left_paint.getStrokeWidth() == tc.border_right_paint.getStrokeWidth()
            && tc.border_left_paint.getStrokeWidth() == tc.border_bottom_paint.getStrokeWidth());

        if (isUniformOutline)
        {
          canvas.drawRoundRect(_tmpRect, r, r, tc.border_left_paint);
        }
        else if (tc.border_left_paint != null && tc.border_left_paint.getStrokeWidth() == 0
            && tc.border_bottom_paint != null && tc.border_bottom_paint.getStrokeWidth() > 0)
        {
          canvas.save();
          float clipTop = y + keyH - tc.border_bottom_paint.getStrokeWidth() - padding;
          canvas.clipRect(x, clipTop, x + keyW, y + keyH);
          canvas.drawRoundRect(_tmpRect, r, r, tc.border_bottom_paint);
          canvas.restore();
        }
        else
        {
          Paint sp = (tc.border_left_paint != null && tc.border_left_paint.getStrokeWidth() > 0)
              ? tc.border_left_paint : tc.border_bottom_paint;
          if (sp != null && sp.getStrokeWidth() > 0)
          {
            canvas.drawRoundRect(_tmpRect, r, r, sp);
            if (tc.border_bottom_paint != null && tc.border_bottom_paint != sp
                && tc.border_bottom_paint.getStrokeWidth() > 0)
            {
              canvas.save();
              float clipTop = y + keyH - tc.border_bottom_paint.getStrokeWidth() - padding;
              canvas.clipRect(x, clipTop, x + keyW, y + keyH);
              canvas.drawRoundRect(_tmpRect, r, r, tc.border_bottom_paint);
              canvas.restore();
            }
          }
          else
          {
            float overlap = Math.max(r, w);
            drawBorder(canvas, x, y, x + overlap, y + keyH, tc.border_left_paint, tc);
            drawBorder(canvas, x + keyW - overlap, y, x + keyW, y + keyH, tc.border_right_paint, tc);
            drawBorder(canvas, x, y, x + keyW, y + overlap, tc.border_top_paint, tc);
            drawBorder(canvas, x, y + keyH - overlap, x + keyW, y + keyH, tc.border_bottom_paint, tc);
          }
        }
      }
    }
  }

  /** Clip to draw a border at a time. This allows to call [drawRoundRect]
      several time with the same parameters but a different Paint. */
  void drawBorder(Canvas canvas, float clipl, float clipt, float clipr,
      float clipb, Paint paint, Theme.Computed.Key tc)
  {
    float r = tc.border_radius;
    canvas.save();
    canvas.clipRect(clipl, clipt, clipr, clipb);
    canvas.drawRoundRect(_tmpRect, r, r, paint);
    canvas.restore();
  }

  private int labelColor(KeyValue k, boolean isKeyDown, boolean sublabel)
  {
    return labelColor(k, isKeyDown, sublabel, (KeyboardData.Key)null);
  }

  private int labelColor(KeyValue k, boolean isKeyDown, boolean sublabel, boolean isActionKey)
  {
    return labelColor(k, isKeyDown, sublabel, (KeyboardData.Key)null);
  }

  private int labelColor(KeyValue k, boolean isKeyDown, boolean sublabel, KeyboardData.Key parentKey)
  {
    int flags = _pointers.getKeyFlags(k);
    boolean isLocked = (flags != -1 && (flags & Pointers.FLAG_P_LOCKED) != 0);
    boolean isLatched = (flags != -1 && (flags & (Pointers.FLAG_P_LATCHED | Pointers.FLAG_P_LATCHABLE)) != 0 && !isLocked);

    boolean isCustom = (_previewTheme != null) || CustomThemeStore.instance(getContext()).isCustomThemeActive(_config);

    if (isLocked)
    {
      if (isCustom)
      {
        if (_previewTheme != null) return _previewTheme.lockedTextColor;
        return CustomThemeStore.instance(getContext()).getLockedTextColor();
      }
      return _theme.lockedColor;
    }

    if (isLatched)
    {
      if (isCustom)
      {
        if (_previewTheme != null) return _previewTheme.activatedTextColor;
        return CustomThemeStore.instance(getContext()).getActivatedTextColor();
      }
      return _theme.activatedColor;
    }

    boolean isFingerDown = (parentKey != null && _pointers.isKeyValueFingerDown(parentKey, k));
    if (isFingerDown)
    {
      if (isCustom)
      {
        if (_previewTheme != null) return _previewTheme.keyPressedTextColor;
        return CustomThemeStore.instance(getContext()).getKeyPressedTextColor();
      }
      return _theme.pressedColor;
    }

    if (sublabel)
    {
      if (isCustom)
      {
        if (_previewTheme != null) return _previewTheme.subLabelColor;
        return CustomThemeStore.instance(getContext()).getSubLabelColor();
      }
      if (k.hasFlagsAny(KeyValue.FLAG_SECONDARY | KeyValue.FLAG_GREYED))
      {
        if (k.hasFlagsAny(KeyValue.FLAG_GREYED))
          return _theme.greyedLabelColor;
        return _theme.secondaryLabelColor;
      }
      return _theme.subLabelColor;
    }

    if (isCustom)
    {
      if (isShiftKey(parentKey))
      {
        if (_previewTheme != null) return _previewTheme.shiftTextColor;
        return CustomThemeStore.instance(getContext()).getShiftTextColor();
      }
      if (isCtrlKey(parentKey))
      {
        if (_previewTheme != null) return _previewTheme.ctrlTextColor;
        return CustomThemeStore.instance(getContext()).getCtrlTextColor();
      }
      if (isEnterKey(parentKey))
      {
        if (_previewTheme != null) return _previewTheme.enterTextColor;
        return CustomThemeStore.instance(getContext()).getEnterTextColor();
      }
      if (isBackspaceKey(parentKey))
      {
        if (_previewTheme != null) return _previewTheme.backspaceTextColor;
        return CustomThemeStore.instance(getContext()).getBackspaceTextColor();
      }
      if (isActionKey(parentKey))
      {
        if (_previewTheme != null) return _previewTheme.actionLabelColor;
        return CustomThemeStore.instance(getContext()).getActionLabelColor();
      }

      if (_previewTheme != null) return _previewTheme.labelColor;
      return CustomThemeStore.instance(getContext()).getLabelColor();
    }

    if (k.hasFlagsAny(KeyValue.FLAG_SECONDARY | KeyValue.FLAG_GREYED))
    {
      if (k.hasFlagsAny(KeyValue.FLAG_GREYED))
        return _theme.greyedLabelColor;
      return _theme.secondaryLabelColor;
    }
    return _theme.labelColor;
  }

  private void drawLabel(Canvas canvas, KeyValue kv, float x, float y,
      float keyH, boolean isKeyDown, Theme.Computed.Key tc)
  {
    drawLabel(canvas, kv, x, y, 0f, keyH, isKeyDown, tc, (KeyboardData.Key)null);
  }

  private void drawLabel(Canvas canvas, KeyValue kv, float x, float y,
      float keyW, float keyH, boolean isKeyDown, Theme.Computed.Key tc)
  {
    drawLabel(canvas, kv, x, y, keyW, keyH, isKeyDown, tc, (KeyboardData.Key)null);
  }

  private void drawLabel(Canvas canvas, KeyValue kv, float x, float y,
      float keyW, float keyH, boolean isKeyDown, Theme.Computed.Key tc, boolean isActionKey)
  {
    drawLabel(canvas, kv, x, y, keyW, keyH, isKeyDown, tc, (KeyboardData.Key)null);
  }

  private void drawLabel(Canvas canvas, KeyValue kv, float x, float y,
      float keyW, float keyH, boolean isKeyDown, Theme.Computed.Key tc, KeyboardData.Key parentKey)
  {
    kv = modifyKey(kv, _mods);
    if (kv == null)
      return;
    float textSize = scaleTextSize(kv, true);
    Paint p = tc.label_paint(kv.hasFlagsAny(KeyValue.FLAG_KEY_FONT), labelColor(kv, isKeyDown, false, parentKey), textSize);
    String str = kv.getString();
    if (str != null && !str.isEmpty() && keyW > 0f)
    {
      float density = getResources().getDisplayMetrics().density;
      float r = (tc != null) ? tc.border_radius : 0f;
      float w = (tc != null) ? tc.border_width : 0f;
      float textWidth = p.measureText(str);
      float maxW = keyW - (r * 0.35f + w) * 2f - 4f * density;
      if (textWidth > maxW && maxW > 0f)
      {
        p.setTextSize(textSize * (maxW / textWidth));
      }
    }
    canvas.drawText(str, x, (keyH - p.ascent() - p.descent()) / 2f + y, p);
  }

  private void drawSpaceBarLabel(Canvas canvas, KeyboardData.Key k, float x, float y,
      float keyW, float keyH, boolean isKeyDown, Theme.Computed.Key tc)
  {
    if (k.keys != null && k.keys[0] != null &&
        k.keys[0].getKind() != KeyValue.Kind.Editing &&
        !k.keys[0].getString().equals(String.valueOf((char)0xE00D)))
    {
      drawLabel(canvas, k.keys[0], keyW / 2f + x, y, keyW, keyH, isKeyDown, tc, k);
      return;
    }

    String labelText = getLayoutDisplayName();
    if (labelText == null || labelText.isEmpty())
    {
      if (k.keys != null && k.keys[0] != null)
        drawLabel(canvas, k.keys[0], keyW / 2f + x, y, keyW, keyH, isKeyDown, tc, k);
      return;
    }

    int color;
    if (isKeyDown)
    {
      if (_previewTheme != null)
        color = _previewTheme.keyPressedTextColor;
      else if (CustomThemeStore.instance(getContext()).isCustomThemeActive(_config))
        color = CustomThemeStore.instance(getContext()).getKeyPressedTextColor();
      else
        color = _theme.pressedColor;
    }
    else if (_previewTheme != null)
    {
      color = _previewTheme.labelColor;
    }
    else if (CustomThemeStore.instance(getContext()).isCustomThemeActive(_config))
    {
      color = CustomThemeStore.instance(getContext()).getLabelColor();
    }
    else
    {
      color = _theme.labelColor;
    }

    float textSize = _mainLabelSize * 0.72f;
    Paint p = tc.label_paint(false, color, textSize);
    float density = getResources().getDisplayMetrics().density;
    float r = (tc != null) ? tc.border_radius : 0f;
    float w = (tc != null) ? tc.border_width : 0f;
    float textWidth = p.measureText(labelText);
    float maxSpaceWidth = keyW - (r * 0.4f + w) * 2f - 6f * density;
    if (textWidth > maxSpaceWidth && maxSpaceWidth > 0f)
    {
      p.setTextSize(textSize * (maxSpaceWidth / textWidth));
    }
    canvas.drawText(labelText, keyW / 2f + x, (keyH - p.ascent() - p.descent()) / 2f + y, p);
  }

  public String getLayoutDisplayName()
  {
    return getLayoutDisplayName(_keyboard);
  }

  public static String getLayoutDisplayName(KeyboardData keyboard)
  {
    if (keyboard == null) return "Space";
    String name = keyboard.name;
    if (name == null || name.trim().isEmpty())
    {
      if ("bengali".equalsIgnoreCase(keyboard.script))
      {
        return "বাংলা";
      }
      return "Space";
    }

    String lower = name.toLowerCase(java.util.Locale.ROOT);

    // 1. Avro Phonetic layout -> অভ্র
    if (lower.contains("avro") || name.contains("অভ্র"))
    {
      return "অভ্র";
    }

    // 2. National / Bijoy layout -> ই-বিজয়
    if (lower.contains("national") || lower.contains("bijoy") || name.contains("জাতীয়") || name.contains("জাতীয়") || name.contains("বিজয়"))
    {
      return "ই-বিজয়";
    }

    // 3. Probhat layout -> প্রভাত
    if (lower.contains("provat") || lower.contains("probhat") || name.contains("প্রভাত"))
    {
      return "প্রভাত";
    }

    // 4. Assamese layout -> অসমীয়া
    if (lower.contains("assamese") || name.contains("অসমীয়া"))
    {
      return "অসমীয়া";
    }

    // 5. English / Latin QWERTY -> English
    if (lower.contains("qwerty") || lower.contains("english") || "latin".equalsIgnoreCase(keyboard.script))
    {
      return "English";
    }

    // 6. Generic Bengali script fallback -> বাংলা
    if ("bengali".equalsIgnoreCase(keyboard.script))
    {
      return "বাংলা";
    }

    // 7. General extraction: if layout name has parentheses e.g. Language (Layout), take inside
    int parenOpen = name.indexOf('(');
    int parenClose = name.indexOf(')');
    if (parenOpen >= 0 && parenClose > parenOpen)
    {
      String inside = name.substring(parenOpen + 1, parenClose).trim();
      if (!inside.isEmpty() && inside.length() <= 12)
      {
        return inside;
      }
    }
    int dash = name.indexOf('-');
    if (dash > 0)
    {
      String part = name.substring(0, dash).trim();
      if (!part.isEmpty() && part.length() <= 12) return part;
    }
    return (name.length() > 12) ? name.substring(0, 12) : name;
  }

  private void drawSubLabel(Canvas canvas, KeyValue kv, float x, float y,
      float keyW, float keyH, int sub_index, boolean isKeyDown,
      Theme.Computed.Key tc)
  {
    drawSubLabel(canvas, kv, x, y, keyW, keyH, sub_index, isKeyDown, tc, false, false, (KeyboardData.Key)null);
  }

  private void drawSubLabel(Canvas canvas, KeyValue kv, float x, float y,
      float keyW, float keyH, int sub_index, boolean isKeyDown,
      Theme.Computed.Key tc, boolean isBottomLeft, boolean isBottomRight)
  {
    drawSubLabel(canvas, kv, x, y, keyW, keyH, sub_index, isKeyDown, tc, isBottomLeft, isBottomRight, (KeyboardData.Key)null);
  }

  private void drawSubLabel(Canvas canvas, KeyValue kv, float x, float y,
      float keyW, float keyH, int sub_index, boolean isKeyDown,
      Theme.Computed.Key tc, boolean isBottomLeft, boolean isBottomRight, boolean isActionKey)
  {
    drawSubLabel(canvas, kv, x, y, keyW, keyH, sub_index, isKeyDown, tc, isBottomLeft, isBottomRight, (KeyboardData.Key)null);
  }

  private void drawSubLabel(Canvas canvas, KeyValue kv, float x, float y,
      float keyW, float keyH, int sub_index, boolean isKeyDown,
      Theme.Computed.Key tc, boolean isBottomLeft, boolean isBottomRight, KeyboardData.Key parentKey)
  {
    Paint.Align a = LABEL_POSITION_H[sub_index];
    Vertical v = LABEL_POSITION_V[sub_index];
    kv = modifyKey(kv, _mods);
    if (kv == null)
      return;

    float density = getResources().getDisplayMetrics().density;
    float r = (tc != null) ? tc.border_radius : 0f;
    float w = (tc != null) ? tc.border_width : 0f;

    // Check if key corner has outer radius (e.g. bottom-left Ctrl, bottom-right Enter)
    float outerRadius = getOuterCornerRadius();
    if (isBottomLeft && a == Paint.Align.LEFT && v == Vertical.BOTTOM && outerRadius > 0)
    {
      r = Math.max(r, outerRadius);
    }
    else if (isBottomRight && a == Paint.Align.RIGHT && v == Vertical.BOTTOM && outerRadius > 0)
    {
      r = Math.max(r, outerRadius);
    }

    boolean isCorner = (a != Paint.Align.CENTER && v != Vertical.CENTER);

    // 1. Dynamic Auto-Scaling:
    // When corner radius increases, rounder pill keys have narrower corner area.
    // Automatically scale down the sublabel font size so it fits comfortably.
    float textSize = scaleTextSize(kv, false);
    if (r > 3f * density)
    {
      float rDp = r / density;
      float scaleFactor = Math.max(0.72f, 1.0f - ((rDp - 3f) / 22f) * 0.28f);
      textSize *= scaleFactor;
    }

    Paint p = tc.sublabel_paint(kv.hasFlagsAny(KeyValue.FLAG_KEY_FONT), labelColor(kv, isKeyDown, true, parentKey), textSize, a);

    // 2. Dynamic Safe Inset:
    // Base padding accounts for standard key padding and border stroke width.
    float basePadding = (_config != null && _config.keyPadding > 0) ? _config.keyPadding : (2f * density);
    if (w > 0f)
    {
      basePadding = Math.max(basePadding, w * 0.6f + 1f * density);
    }

    float padX = basePadding;
    float padY = basePadding;

    if (isCorner && r > 2f * density)
    {
      float extraCornerInset = (r - 2f * density) * 0.36f;
      float maxInsetX = keyW * 0.28f;
      float maxInsetY = keyH * 0.28f;
      padX += Math.min(extraCornerInset, maxInsetX);
      padY += Math.min(extraCornerInset * 0.92f, maxInsetY);
    }
    else if (r > 2f * density)
    {
      if (a != Paint.Align.CENTER)
      {
        padX += Math.min(r * 0.12f, keyW * 0.15f);
      }
      if (v != Vertical.CENTER)
      {
        padY += Math.min(r * 0.12f, keyH * 0.15f);
      }
    }

    if (v == Vertical.CENTER)
      y += (keyH - p.ascent() - p.descent()) / 2f;
    else
      y += (v == Vertical.TOP) ? padY - p.ascent() : keyH - padY - p.descent();

    if (a == Paint.Align.CENTER)
      x += keyW / 2f;
    else
      x += (a == Paint.Align.LEFT) ? padX : keyW - padX;

    String label = kv.getString();
    int label_len = label.length();
    // Limit the label of string keys to 3 characters
    if (label_len > 3 && kv.getKind() == KeyValue.Kind.String)
      label_len = 3;

    // 3. Collision protection: scale down multi-character labels if needed so they don't exceed half-width
    if (a != Paint.Align.CENTER && label_len > 0)
    {
      float textWidth = p.measureText(label, 0, label_len);
      float maxAllowed = (keyW / 2f) - padX;
      if (textWidth > maxAllowed && maxAllowed > 0f)
      {
        p.setTextSize(p.getTextSize() * (maxAllowed / textWidth));
      }
    }

    canvas.drawText(label, 0, label_len, x, y, p);
  }

  private void drawIndication(Canvas canvas, KeyboardData.Key k, float x,
      float y, float keyW, float keyH, Theme.Computed tc)
  {
    if (k.indication == null || k.indication.equals(""))
      return;
    Paint p = tc.indication_paint;
    float density = getResources().getDisplayMetrics().density;
    float r = (tc != null && tc.key != null) ? tc.key.border_radius : 0f;
    float indSize = _subLabelSize;
    if (r > 3f * density)
    {
      float rDp = r / density;
      float scaleFactor = Math.max(0.72f, 1.0f - ((rDp - 3f) / 22f) * 0.28f);
      indSize *= scaleFactor;
    }
    p.setTextSize(indSize);
    canvas.drawText(k.indication, 0, k.indication.length(),
        x + keyW / 2f, (keyH - p.ascent() - p.descent()) * 4/5 + y, p);
  }

  private float scaleTextSize(KeyValue k, boolean main_label)
  {
    float smaller_font = k.hasFlagsAny(KeyValue.FLAG_SMALLER_FONT) ? 0.75f : 1.f;
    float label_size = main_label ? _mainLabelSize : _subLabelSize;
    return label_size * smaller_font;
  }

  public interface OnVisibilityChangeListener
  {
    void onVisibilityChanged(int visibility);
  }

  private OnVisibilityChangeListener _visibilityChangeListener;

  public void setOnVisibilityChangeListener(OnVisibilityChangeListener listener)
  {
    _visibilityChangeListener = listener;
  }

  @Override
  public void setVisibility(int visibility)
  {
    super.setVisibility(visibility);
    if (_visibilityChangeListener != null)
    {
      _visibilityChangeListener.onVisibilityChanged(visibility);
    }
  }
}

