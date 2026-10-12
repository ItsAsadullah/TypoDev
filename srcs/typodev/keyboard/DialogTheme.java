package typodev.keyboard;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * Styling helper that dynamically applies the active keyboard theme to AlertDialogs,
 * including window background, input fields, spinners, radio buttons, and action buttons.
 */
public final class DialogTheme
{
  private DialogTheme() {}

  public static class Palette
  {
    public final int colorKeyboard;
    public final int colorKey;
    public final int colorKeyActivated;
    public final int colorLabel;
    public final int colorSubLabel;

    public final int dialogBg;
    public final int inputBg;
    public final int inputBorder;
    public final int dialogBorder;
    public final int textPrimary;
    public final int textSecondary;
    public final int hintColor;
    public final int accentColor;
    public final boolean isDark;

    public Palette(Context context)
    {
      Context themedContext = context;
      try
      {
        Config cfg = Config.globalConfig();
        if (cfg != null && cfg.theme != 0)
        {
          themedContext = new ContextThemeWrapper(context, cfg.theme);
        }
      }
      catch (Throwable ignored) {}

      int rawKeyboard = resolveColor(themedContext, R.attr.colorKeyboard, Color.parseColor("#251A3A"));
      int rawKey = resolveColor(themedContext, R.attr.colorKey, Color.parseColor("#3B2D50"));
      int rawKeyAct = resolveColor(themedContext, R.attr.colorKeyActivated, Color.parseColor("#DD67527E"));
      int rawLabel = resolveColor(themedContext, R.attr.colorLabel, Color.WHITE);
      int rawSubLabel = resolveColor(themedContext, R.attr.colorSubLabel, Color.parseColor("#B088D3"));

      colorKeyboard = ensureOpaque(rawKeyboard);
      colorKey = ensureOpaque(rawKey);
      colorKeyActivated = ensureOpaque(rawKeyAct);
      colorLabel = rawLabel;
      colorSubLabel = rawSubLabel;

      double darkness = 1.0 - (0.299 * Color.red(colorKeyboard) + 0.587 * Color.green(colorKeyboard) + 0.114 * Color.blue(colorKeyboard)) / 255.0;
      isDark = darkness >= 0.4;

      // Robust accent determination:
      // If colorKeyActivated is too close to colorKeyboard (e.g. both white in White theme),
      // we must find a contrasting accent (like colorLabelActivated #0066cc) or modern fallback.
      double kbLum = 1.0 - darkness;
      double actLum = (0.299 * Color.red(colorKeyActivated) + 0.587 * Color.green(colorKeyActivated) + 0.114 * Color.blue(colorKeyActivated)) / 255.0;
      int chosenAccent = colorKeyActivated;

      if (Math.abs(actLum - kbLum) < 0.22)
      {
        int labelAct = resolveColor(themedContext, R.attr.colorLabelActivated, 0);
        double lblLum = (0.299 * Color.red(labelAct) + 0.587 * Color.green(labelAct) + 0.114 * Color.blue(labelAct)) / 255.0;
        if (labelAct != 0 && Math.abs(lblLum - kbLum) >= 0.22)
        {
          chosenAccent = labelAct;
        }
        else
        {
          chosenAccent = isDark ? Color.parseColor("#60A5FA") : Color.parseColor("#1A73E8");
        }
      }

      if (isDark)
      {
        // Dark theme: match keyboard theme closely with slight surface elevation
        dialogBg = blendColors(colorKeyboard, colorKey, 0.20f);
        inputBg = blendColors(colorKey, Color.WHITE, 0.06f);

        // Make sure accent color is sufficiently bright and readable
        double accLum = (0.299 * Color.red(chosenAccent) + 0.587 * Color.green(chosenAccent) + 0.114 * Color.blue(chosenAccent)) / 255.0;
        int brightenedAccent = (accLum < 0.40) ? blendColors(chosenAccent, Color.WHITE, 0.55f) : chosenAccent;
        accentColor = brightenedAccent;

        inputBorder = adjustAlpha(accentColor, 0.40f);
        dialogBorder = adjustAlpha(accentColor, 0.50f);
        textPrimary = Color.WHITE;
        textSecondary = (colorSubLabel != 0 && colorSubLabel != Color.WHITE) ? colorSubLabel : Color.parseColor("#B088D3");
        hintColor = Color.argb(120, 255, 255, 255);
      }
      else
      {
        // Light theme: crisp white surface with clear borders and soft light-gray inputs
        dialogBg = Color.WHITE;
        inputBg = Color.parseColor("#F3F4F6");
        accentColor = chosenAccent;
        inputBorder = Color.parseColor("#E5E7EB");
        dialogBorder = Color.parseColor("#D1D5DB");
        textPrimary = Color.parseColor("#111827");
        textSecondary = Color.parseColor("#4B5563");
        hintColor = Color.argb(128, 0, 0, 0);
      }
    }
  }

  public static Palette getPalette(Context context)
  {
    return new Palette(context);
  }

  public static int getContrastingTextColor(int backgroundColor)
  {
    double lum = (0.299 * Color.red(backgroundColor) + 0.587 * Color.green(backgroundColor) + 0.114 * Color.blue(backgroundColor)) / 255.0;
    return lum >= 0.55 ? 0xFF111827 : 0xFFFFFFFF;
  }

  /**
   * Replaces the default system dialog window background with a rounded floating card
   * styled precisely to match the keyboard theme.
   */
  public static void applyDialogWindowStyle(AlertDialog dialog, Palette p, float density)
  {
    Window win = dialog.getWindow();
    if (win == null) return;

    GradientDrawable card = new GradientDrawable();
    card.setColor(p.dialogBg);
    card.setCornerRadius(16 * density);
    card.setStroke((int)(1.5f * density), p.dialogBorder);

    int marginH = (int)(18 * density);
    int marginV = (int)(18 * density);
    win.setBackgroundDrawable(new InsetDrawable(card, marginH, marginV, marginH, marginV));

    win.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    win.setDimAmount(0.50f);

    android.view.WindowManager.LayoutParams lp = win.getAttributes();
    lp.gravity = android.view.Gravity.CENTER;
    win.setAttributes(lp);
  }

  /**
   * Styles the title TextView in an AlertDialog to match palette textPrimary.
   */
  public static void styleDialogTitle(AlertDialog dialog, Palette p)
  {
    try
    {
      int titleId = dialog.getContext().getResources().getIdentifier("alertTitle", "id", "android");
      if (titleId > 0)
      {
        TextView tvTitle = dialog.findViewById(titleId);
        if (tvTitle != null)
        {
          tvTitle.setTextColor(p.textPrimary);
          tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        }
      }
    }
    catch (Throwable ignored) {}
  }

  /**
   * Styles the list-item TextViews in an AlertDialog that uses setItems().
   * Call from an OnShowListener so the ListView is already inflated.
   */
  public static void styleDialogListItems(final AlertDialog dialog, final Palette p)
  {
    try
    {
      styleDialogTitle(dialog, p);
      final android.widget.ListView lv = dialog.getListView();
      if (lv == null) return;

      lv.setBackgroundColor(p.dialogBg);
      lv.setDivider(new android.graphics.drawable.ColorDrawable(
          android.graphics.Color.argb(40, 255, 255, 255)));
      lv.setDividerHeight(1);

      // Style all currently visible items
      styleListViewItems(lv, p);

      // Also style items as they scroll into view
      lv.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener()
      {
        @Override
        public boolean onPreDraw()
        {
          try { styleListViewItems(lv, p); } catch (Throwable ignored) {}
          return true;
        }
      });
    }
    catch (Throwable ignored) {}
  }

  private static void styleListViewItems(android.widget.ListView lv, Palette p)
  {
    for (int i = 0; i < lv.getChildCount(); i++)
    {
      View child = lv.getChildAt(i);
      styleTextViewsInView(child, p);
    }
  }

  private static void styleTextViewsInView(View v, Palette p)
  {
    if (v == null) return;
    if (v instanceof TextView)
    {
      TextView tv = (TextView) v;
      tv.setTextColor(p.textPrimary);
      tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15);
      tv.setBackgroundColor(android.graphics.Color.TRANSPARENT);
    }
    else if (v instanceof ViewGroup)
    {
      ViewGroup vg = (ViewGroup) v;
      for (int i = 0; i < vg.getChildCount(); i++)
        styleTextViewsInView(vg.getChildAt(i), p);
    }
  }


  /**
   * Directly styles the AlertDialog action buttons (Save, Cancel) to match the keyboard accent palette.
   */
  public static void styleButtonsNow(AlertDialog dialog, Palette p)
  {
    if (dialog == null) return;
    Button pos = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
    if (pos != null)
    {
      pos.setTextColor(p.accentColor);
      pos.setTypeface(null, Typeface.BOLD);
      pos.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
    }
    Button neg = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
    if (neg != null)
    {
      neg.setTextColor(p.textSecondary);
      neg.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
    }
    Button neu = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
    if (neu != null)
    {
      neu.setTextColor(p.textSecondary);
      neu.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
    }
  }

  /**
   * Styles the AlertDialog action buttons (Save, Cancel) to match the keyboard accent palette on show.
   */
  public static void styleButtons(final AlertDialog dialog, final Palette p)
  {
    dialog.setOnShowListener(new DialogInterface.OnShowListener()
    {
      @Override
      public void onShow(DialogInterface d)
      {
        styleButtonsNow(dialog, p);
      }
    });
  }

  public static void styleEditText(EditText et, Palette p, float density)
  {
    et.setTextColor(p.textPrimary);
    et.setHintTextColor(p.hintColor);
    et.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);

    GradientDrawable gd = new GradientDrawable();
    gd.setColor(p.inputBg);
    gd.setCornerRadius(8 * density);
    gd.setStroke((int)(1 * density), p.inputBorder);
    et.setBackground(gd);

    int padH = (int)(12 * density);
    int padV = (int)(10 * density);
    et.setPadding(padH, padV, padH, padV);
  }

  public static void styleSpinner(Spinner sp, Palette p, float density)
  {
    GradientDrawable gd = new GradientDrawable();
    gd.setColor(p.inputBg);
    gd.setCornerRadius(8 * density);
    gd.setStroke((int)(1 * density), p.inputBorder);
    sp.setBackground(gd);

    int padH = (int)(12 * density);
    int padV = (int)(8 * density);
    sp.setPadding(padH, padV, padH, padV);

    try
    {
      GradientDrawable popupBg = new GradientDrawable();
      popupBg.setColor(p.dialogBg);
      popupBg.setCornerRadius(10 * density);
      popupBg.setStroke((int)(1 * density), p.dialogBorder);
      sp.setPopupBackgroundDrawable(popupBg);
    }
    catch (Throwable ignored) {}
  }

  public static ArrayAdapter<String> createThemedAdapter(
      final Context context,
      String[] items,
      final Palette p,
      final float density)
  {
    ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, items)
    {
      @Override
      public View getView(int position, View convertView, ViewGroup parent)
      {
        View v = super.getView(position, convertView, parent);
        if (v instanceof TextView)
        {
          TextView tv = (TextView) v;
          tv.setTextColor(p.textPrimary);
          tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
          tv.setSingleLine(true);
        }
        return v;
      }

      @Override
      public View getDropDownView(int position, View convertView, ViewGroup parent)
      {
        View v = super.getDropDownView(position, convertView, parent);
        if (v instanceof TextView)
        {
          TextView tv = (TextView) v;
          tv.setTextColor(p.textPrimary);
          tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
          tv.setBackgroundColor(p.dialogBg);
          int pad = (int)(12 * density);
          tv.setPadding(pad, pad, pad, pad);
        }
        return v;
      }
    };
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    return adapter;
  }

  public static void styleRadioButton(RadioButton rb, Palette p)
  {
    rb.setTextColor(p.textPrimary);
    rb.setButtonTintList(ColorStateList.valueOf(p.accentColor));
  }

  public static void styleCheckBox(CheckBox cb, Palette p)
  {
    cb.setTextColor(p.textPrimary);
    cb.setButtonTintList(ColorStateList.valueOf(p.accentColor));
  }

  public static void styleSecondaryButton(Button btn, Palette p, float density)
  {
    btn.setTextColor(p.textPrimary);
    btn.setTypeface(null, Typeface.BOLD);
    GradientDrawable gd = new GradientDrawable();
    gd.setColor(p.inputBg);
    gd.setCornerRadius(8 * density);
    gd.setStroke((int)(1 * density), p.accentColor);
    btn.setBackground(gd);
  }

  public static void stylePrimaryButton(Button btn, Palette p, float density)
  {
    btn.setTextColor(Color.WHITE);
    btn.setTypeface(null, Typeface.BOLD);
    GradientDrawable gd = new GradientDrawable();
    gd.setColor(p.accentColor);
    gd.setCornerRadius(8 * density);
    btn.setBackground(gd);
  }

  public static void styleIconButton(View btn, Palette p, float density)
  {
    GradientDrawable gd = new GradientDrawable();
    gd.setColor(p.inputBg);
    gd.setCornerRadius(6 * density);
    gd.setStroke((int)(1 * density), p.inputBorder);
    btn.setBackground(gd);
  }

  public static int blendColors(int color1, int color2, float ratio)
  {
    float inverse = 1.0f - ratio;
    float r = Color.red(color1) * inverse + Color.red(color2) * ratio;
    float g = Color.green(color1) * inverse + Color.green(color2) * ratio;
    float b = Color.blue(color1) * inverse + Color.blue(color2) * ratio;
    return Color.argb(Color.alpha(color1), (int)r, (int)g, (int)b);
  }

  public static int adjustAlpha(int color, float factor)
  {
    int alpha = Math.round(Color.alpha(color) * factor);
    return Color.argb(Math.min(255, Math.max(0, alpha)), Color.red(color), Color.green(color), Color.blue(color));
  }

  public static int ensureOpaque(int color)
  {
    return Color.rgb(Color.red(color), Color.green(color), Color.blue(color));
  }

  private static int resolveColor(Context ctx, int attr, int fallback)
  {
    try
    {
      TypedValue tv = new TypedValue();
      if (ctx.getTheme().resolveAttribute(attr, tv, true))
      {
        if (tv.type >= TypedValue.TYPE_FIRST_COLOR_INT && tv.type <= TypedValue.TYPE_LAST_COLOR_INT)
          return tv.data;
      }
    }
    catch (Throwable ignored) {}
    return fallback;
  }
}
