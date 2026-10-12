package typodev.keyboard;

import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

public class SpaceBarLayoutLabelTest
{
  private KeyboardData makeLayout(String name, String script)
  {
    return new KeyboardData(Collections.<KeyboardData.Row>emptyList(), 1f, null, script, null, name, true, false, true);
  }

  @Test
  public void testLayoutDisplayNameAvro()
  {
    KeyboardData avro = makeLayout("বাংলা (অভ্র) - Avro Phonetic", "bengali");
    assertEquals("অভ্র", Keyboard2View.getLayoutDisplayName(avro));
  }

  @Test
  public void testLayoutDisplayNameNationalBijoy()
  {
    KeyboardData national = makeLayout("বাংলা (জাতীয় / ই-বিজয়)", "bengali");
    assertEquals("ই-বিজয়", Keyboard2View.getLayoutDisplayName(national));

    KeyboardData nationalOld = makeLayout("বাংলা (জাতীয়)", "bengali");
    assertEquals("ই-বিজয়", Keyboard2View.getLayoutDisplayName(nationalOld));

    KeyboardData bijoy = makeLayout("Bijoy Keyboard", "bengali");
    assertEquals("ই-বিজয়", Keyboard2View.getLayoutDisplayName(bijoy));
  }

  @Test
  public void testLayoutDisplayNameProvat()
  {
    KeyboardData provat = makeLayout("বাংলা (প্রভাত)", "bengali");
    assertEquals("প্রভাত", Keyboard2View.getLayoutDisplayName(provat));
  }

  @Test
  public void testLayoutDisplayNameEnglish()
  {
    KeyboardData qwerty = makeLayout("QWERTY (US)", "latin");
    assertEquals("English", Keyboard2View.getLayoutDisplayName(qwerty));
  }

  @Test
  public void testLayoutDisplayNameNullFallback()
  {
    assertEquals("Space", Keyboard2View.getLayoutDisplayName(null));

    KeyboardData unnamedBengali = makeLayout(null, "bengali");
    assertEquals("বাংলা", Keyboard2View.getLayoutDisplayName(unnamedBengali));
  }
}
