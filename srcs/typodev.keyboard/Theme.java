package typodev.keyboard;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;

public class Theme
{
  // Key colors
  public final int colorKey;
  public final int colorKeyActivated;
  public final int colorKeyAction;
  public final int colorKeySpaceBar;

  // Optional keyboard background gradient
  public final boolean hasKeyboardGradient;
  public final int keyboardGradientStart;
  public final int keyboardGradientEnd;

  // Label colors
  public final int lockedColor;
  public final int activatedColor;
  public final int pressedColor;
  public final int labelColor;
  public final int subLabelColor;
  public final int secondaryLabelColor;
  public final int greyedLabelColor;

  // Key borders
  public final float keyBorderRadius;
  public final float keyBorderWidth;
  public final float keyBorderWidthActivated;
  public final float keyBorderWidthAction;
  public final float keyBorderWidthSpaceBar;
  public final int keyBorderColorLeft;
  public final int keyBorderColorTop;
  public final int keyBorderColorRight;
  public final int keyBorderColorBottom;

  public final int colorNavBar;
  public final boolean isLightNavBar;
  public final Context context;

  public Theme(Context context, AttributeSet attrs)
  {
    this.context = context;
    getKeyFont(context); // _key_font will be accessed
    TypedArray s = context.getTheme().obtainStyledAttributes(attrs, R.styleable.keyboard, 0, 0);
    hasKeyboardGradient = s.hasValue(R.styleable.keyboard_keyboardGradientStart) && s.hasValue(R.styleable.keyboard_keyboardGradientEnd);
    keyboardGradientStart = s.getColor(R.styleable.keyboard_keyboardGradientStart, 0);
    keyboardGradientEnd = s.getColor(R.styleable.keyboard_keyboardGradientEnd, 0);
    colorKey = s.getColor(R.styleable.keyboard_colorKey, 0);
    colorKeyActivated = s.getColor(R.styleable.keyboard_colorKeyActivated, 0);
    colorKeyAction = s.getColor(R.styleable.keyboard_colorKeyAction, colorKey);
    colorKeySpaceBar = s.getColor(R.styleable.keyboard_colorKeySpaceBar, colorKey);
    // colorKeyboard = s.getColor(R.styleable.keyboard_colorKeyboard, 0);
    colorNavBar = s.getColor(R.styleable.keyboard_navigationBarColor, 0);
    isLightNavBar = s.getBoolean(R.styleable.keyboard_windowLightNavigationBar, false);
    labelColor = s.getColor(R.styleable.keyboard_colorLabel, 0);
    activatedColor = s.getColor(R.styleable.keyboard_colorLabelActivated, 0);
    pressedColor = s.getColor(R.styleable.keyboard_colorLabelPressed, labelColor);
    lockedColor = s.getColor(R.styleable.keyboard_colorLabelLocked, 0);
    subLabelColor = s.getColor(R.styleable.keyboard_colorSubLabel, 0);
    secondaryLabelColor = adjustLight(labelColor,
        s.getFloat(R.styleable.keyboard_secondaryDimming, 0.25f));
    greyedLabelColor = adjustLight(labelColor,
        s.getFloat(R.styleable.keyboard_greyedDimming, 0.5f));
    keyBorderRadius = s.getDimension(R.styleable.keyboard_keyBorderRadius, 0);
    keyBorderWidth = s.getDimension(R.styleable.keyboard_keyBorderWidth, 0);
    keyBorderWidthActivated = s.getDimension(R.styleable.keyboard_keyBorderWidthActivated, 0);
    keyBorderWidthAction = s.getDimension(R.styleable.keyboard_keyBorderWidthAction, 0);
    keyBorderWidthSpaceBar = s.getDimension(R.styleable.keyboard_keyBorderWidthSpaceBar, 0);
    keyBorderColorLeft = s.getColor(R.styleable.keyboard_keyBorderColorLeft, colorKey);
    keyBorderColorTop = s.getColor(R.styleable.keyboard_keyBorderColorTop, colorKey);
    keyBorderColorRight = s.getColor(R.styleable.keyboard_keyBorderColorRight, colorKey);
    keyBorderColorBottom = s.getColor(R.styleable.keyboard_keyBorderColorBottom, colorKey);
    s.recycle();
  }

  /** Interpolate the 'value' component toward its opposite by 'alpha'. */
  static int adjustLight(int color, float alpha)
  {
    float[] hsv = new float[3];
    Color.colorToHSV(color, hsv);
    float v = hsv[2];
    hsv[2] = alpha - (2 * alpha - 1) * v;
    return Color.HSVToColor(hsv);
  }

  Paint initIndicationPaint(Paint.Align align, Typeface font)
  {
    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    paint.setTextAlign(align);
    if (font != null)
      paint.setTypeface(font);
    return (paint);
  }

  static Typeface _key_font = null;

  static public Typeface getKeyFont(Context context)
  {
    if (_key_font == null)
      _key_font = Typeface.createFromAsset(context.getAssets(), "special_font.ttf");
    return _key_font;
  }

  public static final class Computed
  {
    public final float vertical_margin;
    public final float horizontal_margin;
    public final float margin_top;
    public final float margin_left;
    public final float row_height;
    public final Paint keyboard_background_paint;
    public final Paint indication_paint;

    public final Key key;
    public final Key key_activated;
    public final Key key_action;
    public final Key key_space_bar;
    public final Key key_suggestion;
    public final Key key_shift;
    public final Key key_ctrl;
    public final Key key_enter;
    public final Key key_backspace;

    public Computed(Theme theme, Config config, float keyWidth, KeyboardData layout)
    {
      this(theme, config, keyWidth, layout, null);
    }

    public Computed(Theme theme, Config config, float keyWidth, KeyboardData layout, Keyboard2View.PreviewThemeOverride pt)
    {
      // Make sure that the layout isn't higher than the screen. Take the
      // height of the candidates view into account.
      row_height = Math.min(config.keyboard_rows_height_pixels,
          (config.screenHeightPixels - config.keyboard_rows_height_pixels) / layout.keysHeight);
      vertical_margin = config.key_vertical_margin * row_height;
      horizontal_margin = config.key_horizontal_margin * keyWidth;
      // Add half of the key margin on the left and on the top as it's also
      // added on the right and on the bottom of every keys.
      margin_top = config.marginTop + vertical_margin / 2;
      margin_left = horizontal_margin / 2;
      float totalHeight = margin_top + row_height * layout.keysHeight + config.margin_bottom;
      if (pt != null)
      {
        if (pt.hasGradient)
          keyboard_background_paint = init_gradient_paint(pt.gradientStart, pt.gradientEnd, totalHeight);
        else
          keyboard_background_paint = null;

        boolean customBorder = pt.borderEnabled;
        float density = theme.context.getResources().getDisplayMetrics().density;
        float customBorderWidth = customBorder ? (pt.borderWidthDp * density) : 0f;
        float customBorderRadius = pt.borderRadiusDp * density;
        int customBorderColor = pt.borderColor;
        boolean bottomBorderEnabled = pt.bottomBorderEnabled;
        int bottomBorderColor = pt.bottomBorderColor;
        float bottomBorderHeight = pt.bottomBorderHeightDp * density;

        key = new Key(theme, config, keyWidth, pt.keyNormal, customBorder, customBorderWidth, customBorderRadius, customBorderColor, bottomBorderEnabled, bottomBorderColor, bottomBorderHeight);
        key_space_bar = new Key(theme, config, keyWidth, pt.keySpace, customBorder, customBorderWidth, customBorderRadius, customBorderColor, bottomBorderEnabled && pt.bottomBorderSpace, bottomBorderColor, bottomBorderHeight);
        key_activated = new Key(theme, config, keyWidth, pt.keyPressedBgColor, customBorder, customBorderWidth, customBorderRadius, customBorderColor, false, 0, 0f);
        key_suggestion = new Key(theme, config, keyWidth, false, KeyboardData.Key.Role.Suggestion);
        key_shift = new Key(theme, config, keyWidth, pt.keyShift, customBorder, customBorderWidth, customBorderRadius, customBorderColor, bottomBorderEnabled && pt.bottomBorderShift, bottomBorderColor, bottomBorderHeight);
        key_ctrl = new Key(theme, config, keyWidth, pt.keyCtrl, customBorder, customBorderWidth, customBorderRadius, customBorderColor, bottomBorderEnabled && pt.bottomBorderCtrl, bottomBorderColor, bottomBorderHeight);
        key_enter = new Key(theme, config, keyWidth, pt.keyEnter, customBorder, customBorderWidth, customBorderRadius, customBorderColor, bottomBorderEnabled && pt.bottomBorderEnter, bottomBorderColor, bottomBorderHeight);
        key_backspace = new Key(theme, config, keyWidth, pt.keyBackspace, customBorder, customBorderWidth, customBorderRadius, customBorderColor, bottomBorderEnabled && pt.bottomBorderBackspace, bottomBorderColor, bottomBorderHeight);
        key_action = key_ctrl;

        indication_paint = init_label_paint(config, null);
        indication_paint.setColor(pt.subLabelColor);
      }
      else
      {
        CustomThemeStore customStore = CustomThemeStore.instance(theme.context);
        boolean isCustom = customStore.isCustomThemeActive(config);
        if (isCustom)
        {
          if (customStore.hasGradient())
            keyboard_background_paint = init_gradient_paint(customStore.getGradientStart(), customStore.getGradientEnd(), totalHeight);
          else
            keyboard_background_paint = null;

          boolean customBorder = customStore.isBorderEnabled();
          float density = theme.context.getResources().getDisplayMetrics().density;
          float customBorderWidth = customBorder ? (customStore.getBorderWidthDp() * density) : 0f;
          float customBorderRadius = customStore.getBorderRadiusDp() * density;
          int customBorderColor = customStore.getBorderColor();
          boolean bottomBorderEnabled = customStore.isBottomBorderEnabled();
          int bottomBorderColor = customStore.getBottomBorderColor();
          float bottomBorderHeight = customStore.getBottomBorderHeightDp() * density;

          key = new Key(theme, config, keyWidth, customStore.getKeyNormal(), customBorder, customBorderWidth, customBorderRadius, customBorderColor, bottomBorderEnabled, bottomBorderColor, bottomBorderHeight);
          key_space_bar = new Key(theme, config, keyWidth, customStore.getKeySpace(), customBorder, customBorderWidth, customBorderRadius, customBorderColor, bottomBorderEnabled && customStore.isBottomBorderSpace(), bottomBorderColor, bottomBorderHeight);
          key_activated = new Key(theme, config, keyWidth, customStore.getKeyPressedBgColor(), customBorder, customBorderWidth, customBorderRadius, customBorderColor, false, 0, 0f);
          key_suggestion = new Key(theme, config, keyWidth, false, KeyboardData.Key.Role.Suggestion);
          key_shift = new Key(theme, config, keyWidth, customStore.getKeyShift(), customBorder, customBorderWidth, customBorderRadius, customBorderColor, bottomBorderEnabled && customStore.isBottomBorderShift(), bottomBorderColor, bottomBorderHeight);
          key_ctrl = new Key(theme, config, keyWidth, customStore.getKeyCtrl(), customBorder, customBorderWidth, customBorderRadius, customBorderColor, bottomBorderEnabled && customStore.isBottomBorderCtrl(), bottomBorderColor, bottomBorderHeight);
          key_enter = new Key(theme, config, keyWidth, customStore.getKeyEnter(), customBorder, customBorderWidth, customBorderRadius, customBorderColor, bottomBorderEnabled && customStore.isBottomBorderEnter(), bottomBorderColor, bottomBorderHeight);
          key_backspace = new Key(theme, config, keyWidth, customStore.getKeyBackspace(), customBorder, customBorderWidth, customBorderRadius, customBorderColor, bottomBorderEnabled && customStore.isBottomBorderBackspace(), bottomBorderColor, bottomBorderHeight);
          key_action = key_ctrl;
        }
        else
        {
          keyboard_background_paint = init_keyboard_background_paint(theme, totalHeight);
          key = new Key(theme, config, keyWidth, false, KeyboardData.Key.Role.Normal);
          key_action = new Key(theme, config, keyWidth, false, KeyboardData.Key.Role.Action);
          key_space_bar = new Key(theme, config, keyWidth, false, KeyboardData.Key.Role.Space_bar);
          key_activated = new Key(theme, config, keyWidth, true, KeyboardData.Key.Role.Normal);
          key_suggestion = new Key(theme, config, keyWidth, false, KeyboardData.Key.Role.Suggestion);
          key_shift = key_action;
          key_ctrl = key_action;
          key_enter = key_action;
          key_backspace = key_action;
        }
        indication_paint = init_label_paint(config, null);
        indication_paint.setColor(isCustom ? customStore.getSubLabelColor() : theme.subLabelColor);
      }
    }

    static Paint init_gradient_paint(int startColor, int endColor, float height)
    {
      Paint p = new Paint();
      p.setShader(new LinearGradient(
              0,
              0,
              0,
              height > 0 ? height : 400,
              startColor,
              endColor,
              Shader.TileMode.CLAMP));
      return p;
    }

    static Paint init_keyboard_background_paint(Theme theme, float height)
    {
      if (!theme.hasKeyboardGradient)
        return null;

      Paint p = new Paint();
      p.setShader(new LinearGradient(
              0,
              0,
              0,
              height,
              theme.keyboardGradientStart,
              theme.keyboardGradientEnd,
              Shader.TileMode.CLAMP));
      return p;
    }

    public static final class Key
    {
      public final Paint bg_paint = new Paint();
      public final Paint border_left_paint;
      public final Paint border_top_paint;
      public final Paint border_right_paint;
      public final Paint border_bottom_paint;
      public final float border_width;
      public final float border_radius;
      final Paint _label_paint;
      final Paint _special_label_paint;
      final Paint _sublabel_paint;
      final Paint _special_sublabel_paint;
      final int _label_alpha_bits;

      public Key(Theme theme, Config config, float keyWidth, boolean activated,
          KeyboardData.Key.Role role)
      {
        border_radius = config.borderConfig ? config.customBorderRadius * keyWidth : theme.keyBorderRadius;
        if (activated)
        {
          border_width = theme.keyBorderWidthActivated;
          bg_paint.setColor(theme.colorKeyActivated);
          bg_paint.setAlpha(config.keyActivatedOpacity);
        }
        else
        {
          switch (role)
          {
            case Action:
              bg_paint.setColor(theme.colorKeyAction);
              border_width = theme.keyBorderWidthAction;
              break;
            case Space_bar:
              bg_paint.setColor(theme.colorKeySpaceBar);
              border_width = theme.keyBorderWidthSpaceBar;
              break;
            case Suggestion:
              bg_paint.setColor(0);
              border_width = 0;
              break;
            default:
              bg_paint.setColor(theme.colorKey);
              border_width = config.borderConfig ? config.customBorderLineWidth : theme.keyBorderWidth;
              break;
          }
          bg_paint.setAlpha(config.keyOpacity);
        }
        border_left_paint = init_border_paint(config, border_width, theme.keyBorderColorLeft);
        border_top_paint = init_border_paint(config, border_width, theme.keyBorderColorTop);
        border_right_paint = init_border_paint(config, border_width, theme.keyBorderColorRight);
        border_bottom_paint = init_border_paint(config, border_width, theme.keyBorderColorBottom);
        _label_paint = init_label_paint(config, null);
        _special_label_paint = init_label_paint(config, _key_font);
        _sublabel_paint = init_label_paint(config, null);
        _special_sublabel_paint = init_label_paint(config, _key_font);
        _label_alpha_bits = (config.labelBrightness & 0xFF) << 24;
      }

      public Key(Theme theme, Config config, float keyWidth, int customBgColor)
      {
        this(theme, config, keyWidth, customBgColor, false, 0f, 0f, theme.keyBorderColorLeft, false, false, 0, 0f);
      }

      public Key(Theme theme, Config config, float keyWidth, int customBgColor,
                 boolean customBorder, float customBorderWidth, float customBorderRadius, int customBorderColor)
      {
        this(theme, config, keyWidth, customBgColor, customBorder, customBorderWidth, customBorderRadius, customBorderColor, false, false, 0, 0f);
      }

      public Key(Theme theme, Config config, float keyWidth, int customBgColor,
                 boolean customBorder, float customBorderWidth, float customBorderRadius, int customBorderColor,
                 boolean isActionOrSpace)
      {
        this(theme, config, keyWidth, customBgColor, customBorder, customBorderWidth, customBorderRadius, customBorderColor, isActionOrSpace, false, 0, 0f);
      }

      public Key(Theme theme, Config config, float keyWidth, int customBgColor,
                 boolean customBorder, float customBorderWidth, float customBorderRadius, int customBorderColor,
                 boolean bottomBorderEnabled, int bottomBorderColor, float bottomBorderHeight)
      {
        this(theme, config, keyWidth, customBgColor, customBorder, customBorderWidth, customBorderRadius, customBorderColor, false, bottomBorderEnabled, bottomBorderColor, bottomBorderHeight);
      }

      public Key(Theme theme, Config config, float keyWidth, int customBgColor,
                 boolean customBorder, float customBorderWidth, float customBorderRadius, int customBorderColor,
                 boolean isActionOrSpace,
                 boolean bottomBorderEnabled, int bottomBorderColor, float bottomBorderHeight)
      {
        float computedWidth;
        if (customBorder)
        {
          border_radius = customBorderRadius;
          int bColor = customBorderColor;
          if (bottomBorderEnabled && bottomBorderHeight > 0)
          {
            border_bottom_paint = init_border_paint(config, bottomBorderHeight, bottomBorderColor);
            computedWidth = Math.max(customBorderWidth, bottomBorderHeight);
          }
          else
          {
            border_bottom_paint = init_border_paint(config, customBorderWidth, bColor);
            computedWidth = customBorderWidth;
          }
          border_left_paint = init_border_paint(config, customBorderWidth, bColor);
          border_top_paint = init_border_paint(config, customBorderWidth, bColor);
          border_right_paint = init_border_paint(config, customBorderWidth, bColor);
        }
        else
        {
          border_radius = customBorderRadius > 0 ? customBorderRadius : (config.borderConfig ? config.customBorderRadius * keyWidth : theme.keyBorderRadius);
          if (config.borderConfig)
          {
            float lineWidth = config.customBorderLineWidth;
            border_left_paint = init_border_paint(config, lineWidth, customBorderColor);
            border_top_paint = init_border_paint(config, lineWidth, customBorderColor);
            border_right_paint = init_border_paint(config, lineWidth, customBorderColor);
            if (bottomBorderEnabled && bottomBorderHeight > 0)
            {
              border_bottom_paint = init_border_paint(config, bottomBorderHeight, bottomBorderColor);
              computedWidth = Math.max(lineWidth, bottomBorderHeight);
            }
            else
            {
              border_bottom_paint = init_border_paint(config, lineWidth, customBorderColor);
              computedWidth = lineWidth;
            }
          }
          else if (bottomBorderEnabled && bottomBorderHeight > 0)
          {
            computedWidth = bottomBorderHeight;
            border_left_paint = init_border_paint(config, 0f, customBgColor);
            border_top_paint = init_border_paint(config, 0f, customBgColor);
            border_right_paint = init_border_paint(config, 0f, customBgColor);
            border_bottom_paint = init_border_paint(config, bottomBorderHeight, bottomBorderColor);
          }
          else
          {
            computedWidth = 0f;
            border_left_paint = init_border_paint(config, 0f, customBgColor);
            border_top_paint = init_border_paint(config, 0f, customBgColor);
            border_right_paint = init_border_paint(config, 0f, customBgColor);
            border_bottom_paint = init_border_paint(config, 0f, customBgColor);
          }
        }
        border_width = computedWidth;
        bg_paint.setColor(customBgColor);
        bg_paint.setAlpha(config.keyOpacity);
        _label_paint = init_label_paint(config, null);
        _special_label_paint = init_label_paint(config, _key_font);
        _sublabel_paint = init_label_paint(config, null);
        _special_sublabel_paint = init_label_paint(config, _key_font);
        _label_alpha_bits = (config.labelBrightness & 0xFF) << 24;
      }

      public Paint label_paint(boolean special_font, int color, float text_size)
      {
        Paint p = special_font ? _special_label_paint : _label_paint;
        p.setColor((color & 0x00FFFFFF) | _label_alpha_bits);
        p.setTextSize(text_size);
        return p;
      }

      public Paint sublabel_paint(boolean special_font, int color, float text_size, Paint.Align align)
      {
        Paint p = special_font ? _special_sublabel_paint : _sublabel_paint;
        p.setColor((color & 0x00FFFFFF) | _label_alpha_bits);
        p.setTextSize(text_size);
        p.setTextAlign(align);
        return p;
      }
    }

    static Paint init_border_paint(Config config, float border_width, int color)
    {
      Paint p = new Paint();
      p.setAlpha(config.keyOpacity);
      p.setStyle(Paint.Style.STROKE);
      p.setStrokeWidth(border_width);
      p.setColor(color);
      return p;
    }

    static Paint init_label_paint(Config config, Typeface font)
    {
      Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
      p.setTextAlign(Paint.Align.CENTER);
      if (font != null)
        p.setTypeface(font);
      return p;
    }
  }
}
