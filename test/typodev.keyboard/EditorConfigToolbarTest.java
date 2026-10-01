package typodev.keyboard;

import android.text.InputType;
import android.view.inputmethod.EditorInfo;
import org.junit.Test;
import typodev.keyboard.suggestions.CandidatesView;
import static org.junit.Assert.*;

public class EditorConfigToolbarTest
{
  @Test
  public void testAcodeToolbarShown()
  {
    // Acode free package with visible password (typical Ace editor setup)
    EditorInfo info = new EditorInfo();
    info.packageName = "com.foxdebug.acodefree";
    info.inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
    assertTrue("CandidatesView should be shown in Acode free", CandidatesView.should_show(info, false, false));
    assertTrue("isCodeEditor should recognize Acode free", CandidatesView.isCodeEditor(info));

    // Acode paid / standard package
    EditorInfo infoPaid = new EditorInfo();
    infoPaid.packageName = "com.foxdebug.acode";
    infoPaid.inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
    assertTrue("CandidatesView should be shown in Acode paid", CandidatesView.should_show(infoPaid, false, false));
    assertTrue("isCodeEditor should recognize Acode", CandidatesView.isCodeEditor(infoPaid));

    // Acode with TYPE_NULL inputClass
    EditorInfo infoNull = new EditorInfo();
    infoNull.packageName = "com.foxdebug.acode";
    infoNull.inputType = InputType.TYPE_NULL;
    assertTrue("CandidatesView should be shown in Acode even if TYPE_NULL", CandidatesView.should_show(infoNull, false, false));

    // Acode with multiple variation bits (containing TYPE_TEXT_VARIATION_PASSWORD)
    EditorInfo infoVariations = new EditorInfo();
    infoVariations.packageName = "com.foxdebug.acode";
    infoVariations.inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;
    assertTrue("CandidatesView should be shown in Acode even if password variation set", CandidatesView.should_show(infoVariations, false, false));
  }

  @Test
  public void testOtherCodeEditorsToolbarShown()
  {
    // Spck Editor
    EditorInfo spck = new EditorInfo();
    spck.packageName = "io.spck";
    spck.inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
    assertTrue(CandidatesView.should_show(spck, false, false));
    assertTrue(CandidatesView.isCodeEditor(spck));

    // QuickEdit
    EditorInfo quickEdit = new EditorInfo();
    quickEdit.packageName = "com.rhmsoft.edit";
    quickEdit.inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
    assertTrue(CandidatesView.should_show(quickEdit, false, false));
    assertTrue(CandidatesView.isCodeEditor(quickEdit));

    // Dcoder
    EditorInfo dcoder = new EditorInfo();
    dcoder.packageName = "com.paprbit.dcoder";
    dcoder.inputType = InputType.TYPE_CLASS_TEXT;
    assertTrue(CandidatesView.should_show(dcoder, false, false));
    assertTrue(CandidatesView.isCodeEditor(dcoder));

    // Godot Editor
    EditorInfo godot = new EditorInfo();
    godot.packageName = "org.godotengine.editor";
    godot.inputType = InputType.TYPE_CLASS_TEXT;
    assertTrue(CandidatesView.should_show(godot, false, false));
    assertTrue(CandidatesView.isCodeEditor(godot));
  }

  @Test
  public void testVisiblePasswordInGenericApp()
  {
    // Any generic app using visible password should show toolbar
    EditorInfo generic = new EditorInfo();
    generic.packageName = "com.unknown.app";
    generic.inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
    assertTrue(CandidatesView.should_show(generic, false, false));
  }

  @Test
  public void testBrowserAndRegularAppsToolbarShown()
  {
    // Chrome browser URL bar / web page
    EditorInfo chrome = new EditorInfo();
    chrome.packageName = "com.android.chrome";
    chrome.inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI;
    assertTrue(CandidatesView.should_show(chrome, false, false));

    // Regular note app
    EditorInfo notes = new EditorInfo();
    notes.packageName = "com.google.android.keep";
    notes.inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE;
    assertTrue(CandidatesView.should_show(notes, false, false));
  }

  @Test
  public void testSecretPasswordEnablesAutofillStrip()
  {
    // Real banking / account login password box
    EditorInfo login = new EditorInfo();
    login.packageName = "com.bank.secureapp";
    login.inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;
    assertTrue("Password fields are detected for autofill support", CandidatesView.isPasswordField(login));
    assertTrue("Genuine password fields show candidates view for password autofill & inline chips", CandidatesView.should_show(login, false, false));

    // Web password field in browser
    EditorInfo webPassword = new EditorInfo();
    webPassword.packageName = "com.android.chrome";
    webPassword.inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD;
    assertTrue("Web password fields are detected for autofill support", CandidatesView.isPasswordField(webPassword));
    assertTrue("Web password fields show candidates view for password autofill & inline chips", CandidatesView.should_show(webPassword, false, false));

    // Test strong password generator
    String generated = typodev.keyboard.autofill.PasswordAutofillHelper.generateStrongPassword(16);
    assertNotNull(generated);
    assertEquals(16, generated.length());
    assertTrue("Generated password has lowercase", generated.matches(".*[a-z].*"));
    assertTrue("Generated password has digit", generated.matches(".*[0-9].*"));
  }

  @Test
  public void testTerminalsAndDeveloperMode()
  {
    // Termux with showInTerminals = false and devMode = false
    EditorInfo termux = new EditorInfo();
    termux.packageName = "com.termux";
    termux.inputType = InputType.TYPE_NULL;
    assertFalse(CandidatesView.should_show(termux, false, false));

    // Termux with showInTerminals = true
    assertTrue(CandidatesView.should_show(termux, true, false));

    // Termux with developerMode = true
    assertTrue(CandidatesView.should_show(termux, false, true));
  }
}
