package typodev.keyboard;

import android.os.Handler;
import android.view.inputmethod.InputConnection;
import java.lang.reflect.Proxy;
import org.junit.Test;
import typodev.keyboard.suggestions.Suggestions;
import static org.junit.Assert.*;

public class NumericCalculatorTest {
  @Test public void arithmeticUsesPrecedenceAndDecimalPrecision() {
    assertEquals("14", NumericCalculator.evaluate("2+3*4"));
    assertEquals("0.3", NumericCalculator.evaluate("0.1+0.2"));
    assertEquals("-3", NumericCalculator.evaluate("5-8"));
    assertEquals("2.5", NumericCalculator.evaluate("10/4"));
    assertEquals("6", NumericCalculator.evaluate("3*-2+12"));
  }

  @Test public void invalidExpressionsNeverProduceAnInsertableResult() {
    for (String value : new String[]{"", "1+", "2..4", "10/0", "2a", "NaN"}) {
      try { NumericCalculator.evaluate(value); fail(value); }
      catch (IllegalArgumentException | ArithmeticException expected) { }
    }
  }

  @Test public void bengaliAndExtendedOperatorsEvaluateCleanly() {
    assertEquals("৬", NumericCalculator.evaluate("২+৪"));
    assertEquals("১৪", NumericCalculator.evaluate("২+৩×৪"));
    assertEquals("৩০", NumericCalculator.evaluate("(১০+৫)*২"));
    assertEquals("৫", NumericCalculator.evaluate("২০÷৪"));
    assertEquals("৮", NumericCalculator.evaluate("২^৩"));
    assertEquals("115", NumericCalculator.evaluate("100+15%"));
    assertEquals("85", NumericCalculator.evaluate("100-15%"));
    assertEquals("30", NumericCalculator.evaluate("200*15%"));
    assertEquals("14", NumericCalculator.evaluate("2(3+4)"));
  }

  @Test public void inlineEvaluationExtractsTrailingMathAndPreservesScript() {
    NumericCalculator.InlineResult bn = NumericCalculator.evaluateInline("২+৪");
    assertNotNull(bn);
    assertEquals("২+৪", bn.matchedText);
    assertEquals("৬", bn.result);
    assertEquals("২+৪=৬", bn.equation);

    NumericCalculator.InlineResult en = NumericCalculator.evaluateInline("2+4");
    assertNotNull(en);
    assertEquals("2+4", en.matchedText);
    assertEquals("6", en.result);
    assertEquals("2+4=6", en.equation);

    NumericCalculator.InlineResult withEq = NumericCalculator.evaluateInline("২ + ৪ =");
    assertNotNull(withEq);
    assertEquals("২ + ৪ =", withEq.matchedText);
    assertEquals("৬", withEq.result);
    assertEquals("২+৪=৬", withEq.equation);

    NumericCalculator.InlineResult inSentence = NumericCalculator.evaluateInline("আজকের বাজার খরচ ১২০+৮০");
    assertNotNull(inSentence);
    assertEquals("১২০+৮০", inSentence.matchedText);
    assertEquals("২০০", inSentence.result);
    assertEquals("১২০+৮০=২০০", inSentence.equation);

    NumericCalculator.InlineResult bullet = NumericCalculator.evaluateInline("- ২+৪");
    assertNotNull(bullet);
    assertEquals("২+৪", bullet.matchedText);
    assertEquals("৬", bullet.result);
    assertEquals("২+৪=৬", bullet.equation);

    for (String nonCalc : new String[]{"", "24", "২৪", "-5", "+8801712345678", "2+", "abc2+4", "2+4 ", "50%"}) {
      assertNull("Should not trigger inline calculator for: " + nonCalc, NumericCalculator.evaluateInline(nonCalc));
    }
  }

  @Test public void suggestionsAndKeyEventHandlerSupportInlineCalculatorEndToEnd() {
    final StringBuilder before = new StringBuilder();
    final InputConnection ic = (InputConnection) Proxy.newProxyInstance(
        InputConnection.class.getClassLoader(),
        new Class<?>[]{InputConnection.class},
        (proxy, method, args) -> {
          switch (method.getName()) {
            case "getTextBeforeCursor":
              int n = (Integer) args[0];
              return before.substring(Math.max(0, before.length() - n));
            case "getTextAfterCursor":
              return "";
            case "getSelectedText":
              return "";
            case "deleteSurroundingText":
              int delBefore = (Integer) args[0];
              if (delBefore > 0 && before.length() >= delBefore) {
                before.setLength(before.length() - delBefore);
              }
              return true;
            case "commitText":
              CharSequence txt = (CharSequence) args[0];
              if (txt != null) before.append(txt);
              return true;
            default:
              return true;
          }
        });

    KeyEventHandler.IReceiver receiver = new KeyEventHandler.IReceiver() {
      @Override public void handle_event_key(KeyValue.Event ev) {}
      @Override public void set_shift_state(boolean state, boolean lock) {}
      @Override public void set_compose_pending(boolean pending) {}
      @Override public void selection_state_changed(boolean ongoing) {}
      @Override public InputConnection getCurrentInputConnection() { return ic; }
      @Override public Handler getHandler() { return null; }
      @Override public void set_suggestions(Suggestions suggestions) {}
      @Override public CharSequence getTextBeforeCursor(int n, int flags) {
        return before.substring(Math.max(0, before.length() - n));
      }
    };

    Config conf = new Config();
    conf.editor_config.should_show_candidates_view = true;
    conf.space_bar_auto_complete = true;

    Suggestions s = new Suggestions(receiver, conf, null);
    KeyEventHandler handler = new KeyEventHandler(receiver, s);
    handler.started(conf);

    // 1. User types "২+৪"
    handler.send_text("২");
    handler.send_text("+");
    handler.send_text("৪");

    assertEquals("২+৪", before.toString());
    assertTrue(s.isCalculatorActive());
    assertFalse("Calculator suggestions must not auto-replace on spacebar", s.should_autocorrect);
    assertTrue(s.count >= 2);
    assertEquals("৬", s.suggestions[0]);
    assertEquals("২+৪=৬", s.suggestions[1]);

    // 2. Tapping "৬" replaces "২+৪" with "৬"
    handler.suggestion_entered("৬");
    assertEquals("৬", before.toString());

    // 3. Chaining "+৫" right after "৬" calculates "১১" and "৬+৫=১১"
    handler.send_text("+");
    handler.send_text("৫");
    assertEquals("৬+৫", before.toString());
    assertEquals("১১", s.suggestions[0]);
    assertEquals("৬+৫=১১", s.suggestions[1]);

    // 4. Tapping "৬+৫=১১" replaces "৬+৫" with "৬+৫=১১"
    handler.suggestion_entered("৬+৫=১১");
    assertEquals("৬+৫=১১", before.toString());

    // 5. Pressing backspace undoes the replacement back to "৬+৫"
    handler.handle_backspace();
    assertEquals("৬+৫", before.toString());

    // 6. Pressing spacebar does not overwrite "৬+৫"
    handler.handle_space_bar();
    assertEquals("৬+৫ ", before.toString());
  }
}
