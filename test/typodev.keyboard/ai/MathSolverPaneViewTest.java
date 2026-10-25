package typodev.keyboard.ai;

import android.content.ClipboardManager;
import android.content.Context;
import android.os.Looper;
import android.text.Editable;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.InputConnection;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import typodev.keyboard.Keyboard2;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class MathSolverPaneViewTest {
  public static class Owner extends Keyboard2 {
    InputConnection editor;
    long session = 1;
    boolean closed;
    @Override public InputConnection getCurrentInputConnection() { return editor; }
    @Override public long getInputSessionId() { return session; }
    @Override public void closeMathSolver() { closed = true; }
    @Override public int getColorLabel() { return 0xffffffff; }
    @Override public int getColorKey() { return 0xff212836; }
    @Override public int getColorKeyboard() { return 0xff151a23; }
    @Override public int getBottomSafety() { return 0; }
  }
  Owner owner;
  Editable contents;
  MathSolverPaneView panel;
  List<GeminiAiService.AiCallback> callbacks;

  @Before public void setup() {
    owner = Robolectric.buildService(Owner.class).get();
    contents = new SpannableStringBuilder();
    Selection.setSelection(contents, 0);
    owner.editor = new BaseInputConnection(new View(owner), true) {
      @Override public Editable getEditable() { return contents; }
    };
    callbacks = new ArrayList<>();
    panel = new MathSolverPaneView(owner, owner, (p, m, cb) -> callbacks.add(cb));
  }

  private void start(String question) {
    contents.replace(0, contents.length(), question);
    Selection.setSelection(contents, contents.length());
    panel.start(question);
  }
  private void tap(String tag) { assertTrue(panel.findViewWithTag(tag).performClick()); }
  private String visible(View v) {
    if (v instanceof TextView) return ((TextView)v).getText().toString() + "\n";
    StringBuilder out = new StringBuilder();
    if (v instanceof ViewGroup) for (int i = 0; i < ((ViewGroup)v).getChildCount(); i++)
      out.append(visible(((ViewGroup)v).getChildAt(i)));
    return out.toString();
  }
  private String response(String marker) {
    return "### চূড়ান্ত উত্তর:\n" + marker + "\n### সমাধান:\n1. " + marker + " working\n### সূত্র ও নিয়ম:\nrule";
  }
  private void deliver(int index, String text) {
    callbacks.get(index).onSuccess(response(text));
    Shadows.shadowOf(Looper.getMainLooper()).idle();
  }

  @Test public void notebookShowsActualWorkingAndSwitchesToExplanationOffline() {
    start("(a+b)^2");
    assertTrue(visible(panel).contains("= a(a + b) + b(a + b)"));
    assertTrue(visible(panel).contains("= a² + ab + ab + b²"));
    tap("math_explained");
    assertTrue(visible(panel).contains("১. বর্গের অর্থ"));
    assertTrue(callbacks.isEmpty());
    tap("math_copy");
    ClipboardManager clipboard = (ClipboardManager)owner.getSystemService(Context.CLIPBOARD_SERVICE);
    assertTrue(clipboard.getPrimaryClip().getItemAt(0).getText().toString().contains("a² + ab + ab + b²"));
  }

  @Test public void staleAnswersCannotOverwriteSelectedModeAndCompletedModesAreCached() {
    start("x+1=2");
    tap("math_explained");
    assertEquals(1, callbacks.size());
    deliver(0, "OBSOLETE");
    assertFalse(visible(panel).contains("OBSOLETE"));
    assertEquals(2, callbacks.size());
    deliver(1, "EXPLAINED");
    assertTrue(visible(panel).contains("EXPLAINED"));
    tap("math_notebook");
    assertTrue(visible(panel).contains("OBSOLETE"));
    tap("math_explained");
    assertEquals(2, callbacks.size());
    assertTrue(visible(panel).contains("EXPLAINED"));
  }

  @Test public void cancellationAndEditorChangeRejectLateAnswers() {
    start("x+1=2");
    panel.cancel();
    deliver(0, "CANCELLED");
    assertFalse(visible(panel).contains("CANCELLED"));
    start("x+2=3");
    owner.session++;
    deliver(1, "WRONG EDITOR");
    assertFalse(visible(panel).contains("WRONG EDITOR"));
  }

  @Test public void insertsOnlyIntoUnchangedOriginalEditor() {
    start("(a+b)^2");
    contents.append(" changed");
    tap("math_insert_answer");
    assertEquals("(a+b)^2 changed", contents.toString());
    assertFalse(owner.closed);
    start("(a+b)^2");
    tap("math_insert_answer");
    assertEquals("a² + 2ab + b²", contents.toString());
    assertTrue(owner.closed);
  }

  @Test public void fullInsertionIncludesWorkingAndSelectionReplacement() {
    contents.append("prefix (a+b)^2 suffix");
    Selection.setSelection(contents, 7, 14);
    panel.start("(a+b)^2");
    tap("math_insert_full");
    assertTrue(contents.toString().startsWith("prefix সমাধান\n"));
    assertTrue(contents.toString().contains("= a² + ab + ab + b²"));
    assertTrue(contents.toString().endsWith(" suffix"));
  }

  @Test public void compactPanelKeepsActionsVisibleAndWorkingScrollable() {
    start("(a+b)^2");
    float density = owner.getResources().getDisplayMetrics().density;
    int width = Math.round(360 * density), height = Math.round(280 * density);
    panel.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
    panel.layout(0, 0, width, height);
    View paper = panel.findViewWithTag("math_working");
    ScrollView scroll = (ScrollView)paper.getParent();
    assertTrue(scroll.getHeight() > 0);
    assertTrue(paper.getHeight() > scroll.getHeight());
    View header = panel.findViewWithTag("math_header");
    for (String tag : new String[]{"math_copy", "math_insert_full", "math_insert_answer"}) {
      View action = panel.findViewWithTag(tag);
      assertSame(header, action.getParent());
      assertNotNull(action.getContentDescription());
      assertTrue(action.getRight() <= width);
    }
    assertTrue(header.getBottom() <= scroll.getTop());
    assertTrue(scroll.getHeight() >= Math.round(170 * density));
  }
}
