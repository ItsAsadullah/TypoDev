package typodev.keyboard;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.widget.TextView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class BanglaFontTest {
  @Test public void onlyBengaliRunsChangeFontAndExistingStylesSurvive() {
    TextView view = new TextView(RuntimeEnvironment.getApplication());
    SpannableString source = new SpannableString("বাংলা x² + 2 = ৪");
    StyleSpan bold = new StyleSpan(Typeface.BOLD);
    source.setSpan(bold, 0, 5, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    view.setText(source);
    BanglaFont.apply(view);
    Spanned styled = (Spanned)view.getText();
    assertEquals(source.toString(), styled.toString());
    BanglaFont.Span[] spans = styled.getSpans(0, styled.length(), BanglaFont.Span.class);
    assertEquals(2, spans.length);
    assertEquals("বাংলা", styled.subSequence(styled.getSpanStart(spans[0]), styled.getSpanEnd(spans[0])).toString());
    assertEquals(0, styled.getSpans(6, 8, BanglaFont.Span.class).length);
    assertEquals(1, styled.getSpans(0, 5, StyleSpan.class).length);
  }

  @Test public void reusedSuggestionUpdatesWithoutDuplicateOrStaleFontSpans() {
    TextView view = new TextView(RuntimeEnvironment.getApplication());
    BanglaFont.apply(view);
    BanglaFont.apply(view);
    view.setText("পরীক্ষা");
    Spanned styled = (Spanned)view.getText();
    assertEquals(1, styled.getSpans(0, styled.length(), BanglaFont.Span.class).length);
    view.setText("English ⌨");
    styled = (Spanned)view.getText();
    assertEquals(0, styled.getSpans(0, styled.length(), BanglaFont.Span.class).length);
  }

  @Test public void keyboardPaintDoesNotLeakFontToLatinOrIconKeys() {
    Paint original = new Paint();
    original.setTypeface(Typeface.DEFAULT_BOLD);
    Paint bangla = BanglaFont.forText(RuntimeEnvironment.getApplication(), original, "ক্ষ");
    assertNotSame(original, bangla);
    assertNotNull(bangla.getTypeface());
    assertSame(Typeface.DEFAULT_BOLD, original.getTypeface());
    assertSame(original, BanglaFont.forText(RuntimeEnvironment.getApplication(), original, "A"));
    assertSame(original, BanglaFont.forText(RuntimeEnvironment.getApplication(), original, "⌨"));
  }
}
