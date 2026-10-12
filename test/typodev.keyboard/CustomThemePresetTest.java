package typodev.keyboard;

import org.junit.Test;
import static org.junit.Assert.*;

public class CustomThemePresetTest {
  @Test
  public void testPresetsContainValidColorsAndSpaceBar() {
    CustomThemeStore.Palette dracula = CustomThemeStore.getDraculaPreset();
    assertNotNull(dracula);
    assertEquals("Dracula", dracula.name);
    assertEquals(CustomThemeStore.DRACULA_KEYBOARD_BG, dracula.keyboardBg);
    assertEquals(CustomThemeStore.DRACULA_KEY_NORMAL, dracula.keySpace);
    assertEquals(0xFF7446A8, dracula.keyShift);
    assertEquals(0xFF7446A8, dracula.keyBackspace);

    CustomThemeStore.Palette system = CustomThemeStore.getSystemPreset(null);
    assertNotNull(system);
    assertEquals("System settings", system.name);
    assertNotEquals(0, system.keyboardBg);
    assertEquals(0xFF262626, system.keyShift);
    assertEquals(0xFF262626, system.keyBackspace);

    CustomThemeStore.Palette dark = CustomThemeStore.getDarkPreset();
    assertNotNull(dark);
    assertEquals("Dark", dark.name);
    assertEquals(0xFF1B1B1B, dark.keyboardBg);
    assertEquals(0xFF262626, dark.keyShift);
    assertEquals(0xFF262626, dark.keyBackspace);
    assertFalse(dark.hasBorder);

    CustomThemeStore.Palette light = CustomThemeStore.getLightPreset();
    assertNotNull(light);
    assertEquals("Light", light.name);
    assertEquals(0xFFE3E3E3, light.keyboardBg);
    assertEquals(0xFFD9D9D9, light.keyShift);
    assertEquals(0xFFD9D9D9, light.keyBackspace);
    assertFalse(light.hasBorder);

    CustomThemeStore.Palette black = CustomThemeStore.getBlackPreset();
    assertNotNull(black);
    assertEquals("Black", black.name);
    assertEquals(0xFF000000, black.keyboardBg);
    assertEquals(0xFF000000, black.keyShift);
    assertEquals(0xFF000000, black.keyBackspace);
    assertFalse(black.hasBorder);

    CustomThemeStore.Palette altBlack = CustomThemeStore.getAltBlackPreset();
    assertNotNull(altBlack);
    assertEquals("Alternative Black", altBlack.name);
    assertTrue(altBlack.hasBorder);
    assertEquals(1.0f, altBlack.borderWidthDp, 0.01f);
    assertEquals(0xFF2A2A2A, altBlack.borderColor);

    CustomThemeStore.Palette white = CustomThemeStore.getWhitePreset();
    assertNotNull(white);
    assertEquals("White", white.name);
    assertTrue(white.hasBorder);

    CustomThemeStore.Palette epaper = CustomThemeStore.getEPaperPreset();
    assertNotNull(epaper);
    assertEquals("ePaper", epaper.name);
    assertEquals(0xFFFFFFFF, epaper.keyboardBg);
    assertEquals(0xFFFFFFFF, epaper.keyShift);
    assertEquals(0xFFFFFFFF, epaper.keyBackspace);
    assertTrue(epaper.hasBorder);
    assertEquals(2.0f, epaper.borderWidthDp, 0.01f);
    assertEquals(0xFF000000, epaper.borderColor);

    CustomThemeStore.Palette desert = CustomThemeStore.getDesertPreset();
    assertNotNull(desert);
    assertEquals("Desert", desert.name);
    assertEquals(0xFFFFE0B2, desert.keyboardBg);
    assertEquals(0xFFFFE9C6, desert.keyShift);
    assertEquals(0xFFFFE9C6, desert.keyBackspace);

    CustomThemeStore.Palette jungle = CustomThemeStore.getJunglePreset();
    assertNotNull(jungle);
    assertEquals("Jungle", jungle.name);
    assertEquals(0xFFB8E3DE, jungle.keyShift);
    assertEquals(0xFFB8E3DE, jungle.keyBackspace);

    CustomThemeStore.Palette rosePine = CustomThemeStore.getRosePinePreset();
    assertNotNull(rosePine);
    assertEquals("Rosé Pine", rosePine.name);
    assertEquals(0xFF2A2740, rosePine.keyShift);
    assertEquals(0xFF2A2740, rosePine.keyBackspace);

    CustomThemeStore.Palette epaperBlack = CustomThemeStore.getEPaperBlackPreset();
    assertNotNull(epaperBlack);
    assertEquals("ePaper Black", epaperBlack.name);
    assertTrue(epaperBlack.hasBorder);
    assertEquals(0xFFFFFFFF, epaperBlack.borderColor);
  }

  @Test
  public void testLockedAndActivatedKeyTextColors() {
    CustomThemeStore.Palette dark = CustomThemeStore.getDarkPreset();
    assertNotNull(dark);
    assertEquals(CustomThemeStore.DEFAULT_LOCKED_TEXT_COLOR, dark.lockedTextColor);
    assertEquals(CustomThemeStore.DEFAULT_ACTIVATED_TEXT_COLOR, dark.activatedTextColor);

    CustomThemeStore.Palette dracula = CustomThemeStore.getDraculaPreset();
    assertNotNull(dracula);
    assertEquals(CustomThemeStore.DEFAULT_LOCKED_TEXT_COLOR, dracula.lockedTextColor);
    assertEquals(CustomThemeStore.DEFAULT_ACTIVATED_TEXT_COLOR, dracula.activatedTextColor);
  }
}
