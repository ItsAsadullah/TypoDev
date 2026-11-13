-keep public class juloo.cdict.* {
  public protected private *;
}

# Keep XML-inflated custom android.preference.Preference subclasses
-keep class typodev.keyboard.prefs.** {
  public <init>(...);
}

# Keep XML-inflated custom View subclasses across keyboard packages
-keep public class typodev.keyboard.** extends android.view.View {
  public <init>(android.content.Context);
  public <init>(android.content.Context, android.util.AttributeSet);
  public <init>(android.content.Context, android.util.AttributeSet, int);
}
