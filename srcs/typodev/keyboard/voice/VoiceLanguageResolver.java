package typodev.keyboard.voice;

/** Resolve the spoken language from the layout rather than the app interface language. */
public final class VoiceLanguageResolver {
  private VoiceLanguageResolver() {}

  public static String resolve(String script, String name) {
    String layoutScript = script == null ? "" : script.toLowerCase(java.util.Locale.ROOT);
    String layoutName = name == null ? "" : name.toLowerCase(java.util.Locale.ROOT);
    if (layoutScript.contains("beng") || layoutName.contains("বাংলা") || layoutName.contains("bengali"))
      return "bn-BD";
    // Several languages share Arabic letters; resolve their names before the script.
    if (layoutScript.contains("persian") || layoutName.contains("persian") || layoutName.contains("فارسی"))
      return "fa-IR";
    if (layoutScript.contains("urdu") || layoutName.contains("urdu") || layoutName.contains("اردو"))
      return "ur-PK";
    if (layoutName.contains("kurdish")) return "ckb-IQ";
    if (layoutName.contains("talysh")) return "tly-AZ";
    if (layoutScript.equals("arabic") || layoutScript.equals("arab")
        || layoutName.contains("arabic") || layoutName.contains("العربية"))
      return "ar-SA";
    return "en-US";
  }
}
