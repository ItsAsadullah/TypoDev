package typodev.keyboard.voice;

/** Resolve the spoken language from the layout rather than the app interface language. */
public final class VoiceLanguageResolver {
  private VoiceLanguageResolver() {}

  public static String resolve(String script, String name) {
    String layoutScript = script == null ? "" : script.toLowerCase(java.util.Locale.ROOT);
    String layoutName = name == null ? "" : name.toLowerCase(java.util.Locale.ROOT);
    if (layoutScript.contains("beng") || layoutName.contains("বাংলা") || layoutName.contains("bengali")
        || layoutName.contains("avro") || layoutName.contains("অভ্র") || layoutName.contains("banglish")
        || layoutName.contains("provat") || layoutName.contains("প্রভাত")
        || layoutName.contains("জাতীয়") || layoutName.contains("জাতীয়"))
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
    if (layoutScript.contains("devanagari") || layoutScript.contains("deva")
        || layoutName.contains("hindi") || layoutName.contains("हिन्दी") || layoutName.contains("हिंदी"))
      return "hi-IN";
    if (layoutName.contains("ukrain") || layoutName.contains("україн"))
      return "uk-UA";
    if (layoutName.contains("russian") || layoutName.contains("русский")
        || layoutScript.equals("cyrillic") || layoutScript.equals("cyrl"))
      return "ru-RU";
    if (layoutName.contains("spanish") || layoutName.contains("español"))
      return "es-ES";
    if (layoutName.contains("french") || layoutName.contains("français") || layoutName.contains("azerty"))
      return "fr-FR";
    if (layoutName.contains("german") || layoutName.contains("deutsch") || layoutName.contains("qwertz"))
      return "de-DE";
    if (layoutName.contains("turkish") || layoutName.contains("türkçe"))
      return "tr-TR";
    if (layoutName.contains("indonesian") || layoutName.contains("bahasa indonesia"))
      return "id-ID";
    if (layoutName.contains("portuguese") || layoutName.contains("português"))
      return "pt-BR";
    return "en-US";
  }
}
