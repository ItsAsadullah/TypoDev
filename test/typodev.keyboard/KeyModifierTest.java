package typodev.keyboard;

import typodev.keyboard.KeyModifier;
import typodev.keyboard.KeyValue;
import org.junit.Test;
import static typodev.keyboard.TestUtils.*;
import static org.junit.Assert.*;

public class KeyModifierTest
{
  public KeyModifierTest() {}

  @Test
  public void compose() throws Exception
  {
    assertEquals(eval("compose", "space", "space"), key("nbsp"));
    assertEquals(eval("compose", "-", "space"), str("~"));
    assertEquals(eval("compose", "space", "-"), str("~"));
  }

  @Test
  public void testSuperscriptLatinAndBengali()
  {
    // Letter q must NOT return tofu \uA7F4, but \u1D60
    KeyValue supQ = KeyModifier.modify(KeyValue.makeCharKey('q'), KeyValue.Modifier.SUPERSCRIPT);
    assertNotNull(supQ);
    assertEquals('\u1D60', supQ.getChar());

    // Standard Latin superscripts
    KeyValue supA = KeyModifier.modify(KeyValue.makeCharKey('a'), KeyValue.Modifier.SUPERSCRIPT);
    assertEquals('ᵃ', supA.getChar());

    // Bengali numerals in superscript
    KeyValue supBn1 = KeyModifier.modify(KeyValue.makeCharKey('১'), KeyValue.Modifier.SUPERSCRIPT);
    assertEquals('¹', supBn1.getChar());

    KeyValue supBn2 = KeyModifier.modify(KeyValue.makeCharKey('২'), KeyValue.Modifier.SUPERSCRIPT);
    assertEquals('²', supBn2.getChar());

    KeyValue supBn0 = KeyModifier.modify(KeyValue.makeCharKey('০'), KeyValue.Modifier.SUPERSCRIPT);
    assertEquals('⁰', supBn0.getChar());

    // Bengali character in superscript
    KeyValue supKa = KeyModifier.modify(KeyValue.makeCharKey('ক'), KeyValue.Modifier.SUPERSCRIPT);
    assertEquals("^ক", supKa.getString());
  }

  @Test
  public void testSubscriptLatinAndBengali()
  {
    // Subscripts that were previously missing
    KeyValue subD = KeyModifier.modify(KeyValue.makeCharKey('d'), KeyValue.Modifier.SUBSCRIPT);
    assertNotNull(subD);
    assertEquals('\u146F', subD.getChar());

    KeyValue subF = KeyModifier.modify(KeyValue.makeCharKey('f'), KeyValue.Modifier.SUBSCRIPT);
    assertNotNull(subF);
    assertEquals('\u0562', subF.getChar());

    KeyValue subG = KeyModifier.modify(KeyValue.makeCharKey('g'), KeyValue.Modifier.SUBSCRIPT);
    assertNotNull(subG);
    assertEquals('\u2089', subG.getChar());

    KeyValue subB = KeyModifier.modify(KeyValue.makeCharKey('b'), KeyValue.Modifier.SUBSCRIPT);
    assertNotNull(subB);
    assertEquals('ᵦ', subB.getChar());

    KeyValue subH = KeyModifier.modify(KeyValue.makeCharKey('h'), KeyValue.Modifier.SUBSCRIPT);
    assertNotNull(subH);
    assertEquals('ₕ', subH.getChar());

    // Bengali numerals in subscript
    KeyValue subBn1 = KeyModifier.modify(KeyValue.makeCharKey('১'), KeyValue.Modifier.SUBSCRIPT);
    assertEquals('₁', subBn1.getChar());

    KeyValue subBn2 = KeyModifier.modify(KeyValue.makeCharKey('২'), KeyValue.Modifier.SUBSCRIPT);
    assertEquals('₂', subBn2.getChar());

    // Bengali character in subscript
    KeyValue subKa = KeyModifier.modify(KeyValue.makeCharKey('ক'), KeyValue.Modifier.SUBSCRIPT);
    assertEquals("_ক", subKa.getString());
  }
}
