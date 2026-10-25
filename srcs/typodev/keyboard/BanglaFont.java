package typodev.keyboard;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.style.MetricAffectingSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/** Uses the bundled Kalpurush only for Bengali runs; other scripts and icons keep their fonts. */
public final class BanglaFont {
  private static Typeface regular;
  private static Typeface bold;
  private BanglaFont() {}

  public static synchronized Typeface get(Context context) {
    if (regular == null) regular = Typeface.createFromAsset(context.getAssets(), "fonts/kalpurush.ttf");
    return regular;
  }

  private static synchronized Typeface getForStyle(Typeface base, int style) {
    if (style == Typeface.NORMAL || style == 0) return base;
    if (style == Typeface.BOLD && base == regular) {
      if (bold == null) bold = Typeface.create(base, Typeface.BOLD);
      return bold;
    }
    return Typeface.create(base, style);
  }

  private static boolean bengali(char c) { return c >= '\u0980' && c <= '\u09ff'; }
  public static boolean containsBengali(CharSequence text) {
    if (text != null) for (int i = 0; i < text.length(); i++) if (bengali(text.charAt(i))) return true;
    return false;
  }

  /** Never mutate a shared theme Paint: the next key may be Latin or an icon. */
  public static Paint forText(Context context, Paint original, CharSequence text) {
    if (!containsBengali(text)) return original;
    Paint paint = new Paint(original);
    int style = original.getTypeface() == null ? Typeface.NORMAL : original.getTypeface().getStyle();
    paint.setTypeface(getForStyle(get(context), style));
    return paint;
  }

  public static final class Span extends MetricAffectingSpan {
    private final Typeface font;
    Span(Typeface font) { this.font = font; }
    private void apply(TextPaint paint) {
      int style = paint.getTypeface() == null ? Typeface.NORMAL : paint.getTypeface().getStyle();
      paint.setTypeface(getForStyle(font, style));
    }
    @Override public void updateMeasureState(TextPaint paint) { apply(paint); }
    @Override public void updateDrawState(TextPaint paint) { apply(paint); }
  }

  private static void style(Spannable text, Typeface font) {
    if (text == null) return;
    Span[] existing = text.getSpans(0, text.length(), Span.class);
    if (!containsBengali(text)) {
      if (existing != null && existing.length > 0) {
        for (Span span : existing) text.removeSpan(span);
      }
      return;
    }
    if (existing != null && existing.length > 0) {
      for (Span span : existing) text.removeSpan(span);
    }
    for (int i = 0; i < text.length();) {
      if (!bengali(text.charAt(i))) { i++; continue; }
      int start = i++;
      while (i < text.length() && (bengali(text.charAt(i)) || text.charAt(i) == '\u200c' || text.charAt(i) == '\u200d')) i++;
      text.setSpan(new Span(font), start, i, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
  }

  public static void apply(TextView view) {
    if (view.getTag(R.id.bangla_font_bound) != null) return;
    view.setTag(R.id.bangla_font_bound, Boolean.TRUE);
    Typeface font = get(view.getContext());
    CharSequence initial = view.getText();
    if (containsBengali(initial)) {
      if (initial instanceof Spannable) style((Spannable)initial, font);
      else {
        SpannableString styled = new SpannableString(initial);
        style(styled, font);
        view.setText(styled, TextView.BufferType.SPANNABLE);
      }
    }
    view.addTextChangedListener(new TextWatcher() {
      private boolean updating = false;
      @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
      @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
      @Override public void afterTextChanged(Editable text) {
        if (updating) return;
        updating = true;
        try {
          style(text, font);
        } finally {
          updating = false;
        }
      }
    });
  }

  private static void applyTree(View root) {
    if (root instanceof TextView) apply((TextView)root);
    if (root instanceof ViewGroup) {
      ViewGroup group = (ViewGroup)root;
      for (int i = 0; i < group.getChildCount(); i++) applyTree(group.getChildAt(i));
    }
  }

  /** Also covers dynamically added suggestions, clipboard and AI panels. */
  public static void install(View root) {
    applyTree(root);
    if (root.getTag(R.id.bangla_font_root) != null) return;
    root.setTag(R.id.bangla_font_root, Boolean.TRUE);
    View.OnAttachStateChangeListener lifecycle = new View.OnAttachStateChangeListener() {
      private final android.view.ViewTreeObserver.OnGlobalLayoutListener listener = () -> applyTree(root);
      @Override public void onViewAttachedToWindow(View v) {
        applyTree(root);
        v.getViewTreeObserver().addOnGlobalLayoutListener(listener);
      }
      @Override public void onViewDetachedFromWindow(View v) {
        v.getViewTreeObserver().removeOnGlobalLayoutListener(listener);
      }
    };
    root.addOnAttachStateChangeListener(lifecycle);
    if (root.isAttachedToWindow()) lifecycle.onViewAttachedToWindow(root);
  }
}
