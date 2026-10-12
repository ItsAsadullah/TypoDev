package typodev.keyboard;

import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.TextView;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk = 34)
public class NumericPadViewTest {
  public static class Owner extends Keyboard2 {
    InputConnection connection;
    @Override public InputConnection getCurrentInputConnection() { return connection; }
    @Override public int getColorKeyboard() { return 0xff151a23; }
    @Override public int getColorKey() { return 0xff212836; }
    @Override public int getColorLabel() { return 0xffffffff; }
    @Override public int getColorKeyActivated() { return 0xff2aabee; }
  }
  private final List<String> commits = new ArrayList<>();
  private Owner owner() {
    Owner owner = Robolectric.buildService(Owner.class).get();
    owner.connection = (InputConnection)Proxy.newProxyInstance(InputConnection.class.getClassLoader(),
        new Class<?>[]{InputConnection.class}, (proxy, method, args) -> {
          if (method.getName().equals("commitText")) commits.add(args[0].toString());
          if (method.getReturnType() == boolean.class) return true;
          if (method.getReturnType() == int.class) return 0;
          return null;
        });
    return owner;
  }
  private EditorInfo editor(int flags) { EditorInfo e = new EditorInfo(); e.inputType = InputType.TYPE_CLASS_NUMBER | flags; return e; }
  private View find(View view, String text) {
    if (view instanceof TextView && text.contentEquals(((TextView)view).getText())) return view;
    if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup)view).getChildCount(); i++) {
      View match = find(((ViewGroup)view).getChildAt(i), text); if (match != null) return match;
    }
    return null;
  }
  private void tap(View view, String text) { View key = find(view, text); assertNotNull(text, key); assertTrue(key.performClick()); }
  @Test public void englishIsDefaultAndLanguageResetsOnReopen() {
    Owner owner = owner(); NumericPadView pad = new NumericPadView(owner, editor(0));
    tap(pad, "1"); tap(pad, "বাংলা"); tap(pad, "২");
    pad = new NumericPadView(owner, editor(0)); tap(pad, "3");
    assertEquals(java.util.Arrays.asList("1", "২", "3"), commits);
    pad.measure(View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.AT_MOST));
    assertEquals(600, pad.getMeasuredWidth());
    ViewGroup row = (ViewGroup)pad.getChildAt(2);
    assertTrue(Math.abs(row.getChildAt(0).getMeasuredWidth() - row.getChildAt(3).getMeasuredWidth()) <= 1);
  }
  @Test public void calculatorClearAndBackspaceAlsoWorkOnExistingEditorText() {
    Owner owner = owner();
    android.text.Editable text = new android.text.SpannableStringBuilder("100");
    android.text.Selection.setSelection(text, text.length());
    owner.connection = new android.view.inputmethod.BaseInputConnection(new View(owner), true) {
      @Override public android.text.Editable getEditable() { return text; }
    };
    NumericPadView pad = new NumericPadView(owner, editor(0));
    assertNull(find(pad, "⌄"));
    tap(pad, "Calculator"); tap(pad, "⌫");
    assertEquals("10", text.toString());
    tap(pad, "7"); tap(pad, "⌫");
    assertEquals("10", text.toString());
    tap(pad, "C"); assertEquals("", text.toString());
    text.append("1234"); android.text.Selection.setSelection(text, 1, 3);
    tap(pad, "⌫"); assertEquals("14", text.toString());
    tap(pad, "C"); assertEquals("", text.toString());
  }
  @Test public void calculatorCommitsOnlyTheResultAndRespectsFieldRestrictions() {
    NumericPadView pad = new NumericPadView(owner(), editor(InputType.TYPE_NUMBER_FLAG_DECIMAL));
    tap(pad, "Calculator"); tap(pad, "1"); tap(pad, "+"); tap(pad, "2");
    assertTrue(commits.isEmpty()); tap(pad, "="); assertEquals(java.util.Arrays.asList("3"), commits);
    tap(pad, "Calculator"); tap(pad, "1"); tap(pad, "÷"); tap(pad, "0"); tap(pad, "=");
    assertEquals(1, commits.size());
    NumericPadView integerPad = new NumericPadView(owner(), editor(0));
    tap(integerPad, "Calculator"); tap(integerPad, "1"); tap(integerPad, "÷"); tap(integerPad, "2"); tap(integerPad, "=");
    assertEquals(1, commits.size());
    assertNull(find(new NumericPadView(owner(), editor(InputType.TYPE_NUMBER_VARIATION_PASSWORD)), "Calculator"));
  }
  @Test @org.robolectric.annotation.Config(sdk = 35)
  public void systemBarsStayBelowBothPadsWithoutAccumulatingPadding() {
    NumericPadView pad = new NumericPadView(owner(), editor(0));
    int base = pad.getPaddingBottom();
    android.view.WindowInsets insets = new android.view.WindowInsets.Builder()
        .setInsets(android.view.WindowInsets.Type.navigationBars(), android.graphics.Insets.of(12, 0, 8, 36)).build();
    pad.onApplyWindowInsets(insets);
    assertEquals(base + 36, pad.getPaddingBottom());
    assertEquals(base + 12, pad.getPaddingLeft());
    assertEquals(base + 8, pad.getPaddingRight());
    tap(pad, "Calculator");
    pad.onApplyWindowInsets(insets);
    assertEquals(base + 36, pad.getPaddingBottom());
    pad.onApplyWindowInsets(new android.view.WindowInsets.Builder()
        .setInsets(android.view.WindowInsets.Type.navigationBars(), android.graphics.Insets.NONE).build());
    assertEquals(base, pad.getPaddingBottom());
  }
}
